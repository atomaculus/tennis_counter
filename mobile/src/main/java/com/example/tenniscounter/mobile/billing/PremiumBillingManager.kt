package com.example.tenniscounter.mobile.billing

import android.app.Activity
import android.content.Context
import android.util.Log
import com.android.billingclient.api.AcknowledgePurchaseParams
import com.android.billingclient.api.BillingClient
import com.android.billingclient.api.BillingClient.BillingResponseCode
import com.android.billingclient.api.BillingClient.ProductType
import com.android.billingclient.api.BillingClientStateListener
import com.android.billingclient.api.BillingFlowParams
import com.android.billingclient.api.BillingResult
import com.android.billingclient.api.ProductDetails
import com.android.billingclient.api.Purchase
import com.android.billingclient.api.PurchasesUpdatedListener
import com.android.billingclient.api.QueryProductDetailsParams
import com.android.billingclient.api.QueryPurchasesParams
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

data class PremiumUiState(
    val isPremiumUnlocked: Boolean = false,
    val isBillingReady: Boolean = false,
    val isLoading: Boolean = true,
    val isPurchaseInProgress: Boolean = false,
    val productPriceLabel: String? = null,
    val message: String? = null
)

class PremiumBillingManager(
    context: Context
) : PurchasesUpdatedListener {
    private val appContext = context.applicationContext
    private val billingClient = BillingClient.newBuilder(appContext)
        .setListener(this)
        .enablePendingPurchases()
        .build()

    private var productDetails: ProductDetails? = null

    private val _uiState = MutableStateFlow(
        PremiumUiState(
            isPremiumUnlocked = PremiumAccessStore.isPremiumUnlocked(appContext),
            isLoading = true
        )
    )
    val uiState: StateFlow<PremiumUiState> = _uiState.asStateFlow()

    fun start() {
        connectIfNeeded()
    }

    fun dispose() {
        if (billingClient.isReady) {
            billingClient.endConnection()
        }
    }

    fun launchPurchase(activity: Activity) {
        clearMessage()
        if (!billingClient.isReady) {
            _uiState.update { it.copy(message = "Billing is connecting, try again in a moment.") }
            connectIfNeeded()
            return
        }
        val details = productDetails
        if (details == null) {
            _uiState.update { it.copy(message = "Premium is not available yet. Try again.") }
            queryProductDetails()
            return
        }

        _uiState.update { it.copy(isPurchaseInProgress = true) }
        val params = BillingFlowParams.newBuilder()
            .setProductDetailsParamsList(
                listOf(
                    BillingFlowParams.ProductDetailsParams.newBuilder()
                        .setProductDetails(details)
                        .build()
                )
            )
            .build()

        val result = billingClient.launchBillingFlow(activity, params)
        if (result.responseCode != BillingResponseCode.OK) {
            _uiState.update {
                it.copy(
                    isPurchaseInProgress = false,
                    message = result.debugMessage.ifBlank { "Could not start purchase flow." }
                )
            }
        }
    }

    fun restorePurchases() {
        clearMessage()
        if (!billingClient.isReady) {
            _uiState.update { it.copy(message = "Billing is connecting, try again in a moment.") }
            connectIfNeeded()
            return
        }
        queryPurchases()
    }

    override fun onPurchasesUpdated(billingResult: BillingResult, purchases: MutableList<Purchase>?) {
        when (billingResult.responseCode) {
            BillingResponseCode.OK -> {
                _uiState.update { it.copy(isPurchaseInProgress = false) }
                if (!purchases.isNullOrEmpty()) {
                    processPurchases(purchases)
                } else {
                    queryPurchases()
                }
            }
            BillingResponseCode.USER_CANCELED -> {
                _uiState.update {
                    it.copy(isPurchaseInProgress = false, message = "Purchase canceled.")
                }
            }
            else -> {
                _uiState.update {
                    it.copy(
                        isPurchaseInProgress = false,
                        message = billingResult.debugMessage.ifBlank { "Purchase failed." }
                    )
                }
            }
        }
    }

    private fun connectIfNeeded() {
        if (billingClient.isReady) {
            _uiState.update { it.copy(isBillingReady = true, isLoading = false) }
            queryProductDetails()
            queryPurchases()
            return
        }
        _uiState.update { it.copy(isLoading = true) }
        billingClient.startConnection(object : BillingClientStateListener {
            override fun onBillingSetupFinished(billingResult: BillingResult) {
                if (billingResult.responseCode == BillingResponseCode.OK) {
                    _uiState.update { it.copy(isBillingReady = true, isLoading = false) }
                    queryProductDetails()
                    queryPurchases()
                } else {
                    _uiState.update {
                        it.copy(
                            isBillingReady = false,
                            isLoading = false,
                            message = billingResult.debugMessage.ifBlank { "Billing setup failed." }
                        )
                    }
                }
            }

            override fun onBillingServiceDisconnected() {
                _uiState.update { it.copy(isBillingReady = false) }
            }
        })
    }

    private fun queryProductDetails() {
        val params = QueryProductDetailsParams.newBuilder()
            .setProductList(
                listOf(
                    QueryProductDetailsParams.Product.newBuilder()
                        .setProductId(PREMIUM_PRODUCT_ID)
                        .setProductType(ProductType.INAPP)
                        .build()
                )
            )
            .build()

        billingClient.queryProductDetailsAsync(params) { billingResult, products ->
            if (billingResult.responseCode != BillingResponseCode.OK) {
                _uiState.update {
                    it.copy(message = billingResult.debugMessage.ifBlank { "Could not load premium price." })
                }
                return@queryProductDetailsAsync
            }
            productDetails = products.firstOrNull { it.productId == PREMIUM_PRODUCT_ID }
            _uiState.update {
                it.copy(
                    productPriceLabel = productDetails?.oneTimePurchaseOfferDetails?.formattedPrice
                )
            }
        }
    }

    private fun queryPurchases() {
        val params = QueryPurchasesParams.newBuilder()
            .setProductType(ProductType.INAPP)
            .build()
        billingClient.queryPurchasesAsync(params) { billingResult, purchases ->
            if (billingResult.responseCode != BillingResponseCode.OK) {
                Log.w(TAG, "queryPurchasesAsync failed: ${billingResult.debugMessage}")
                _uiState.update {
                    it.copy(message = billingResult.debugMessage.ifBlank { "Could not restore purchases." })
                }
                return@queryPurchasesAsync
            }
            processPurchases(purchases)
        }
    }

    private fun processPurchases(purchases: List<Purchase>) {
        val premiumPurchase = purchases.firstOrNull { purchase ->
            purchase.products.contains(PREMIUM_PRODUCT_ID) &&
                purchase.purchaseState == Purchase.PurchaseState.PURCHASED
        }

        val unlocked = premiumPurchase != null
        PremiumAccessStore.setPremiumUnlocked(appContext, unlocked)
        _uiState.update { it.copy(isPremiumUnlocked = unlocked) }

        premiumPurchase?.let { purchase ->
            if (!purchase.isAcknowledged) {
                val ackParams = AcknowledgePurchaseParams.newBuilder()
                    .setPurchaseToken(purchase.purchaseToken)
                    .build()
                billingClient.acknowledgePurchase(ackParams) { result ->
                    if (result.responseCode != BillingResponseCode.OK) {
                        Log.w(TAG, "acknowledgePurchase failed: ${result.debugMessage}")
                    }
                }
            }
        }
    }

    private fun clearMessage() {
        _uiState.update { it.copy(message = null) }
    }

    companion object {
        const val PREMIUM_PRODUCT_ID = "premium_unlock"
        private const val TAG = "PremiumBilling"
    }
}
