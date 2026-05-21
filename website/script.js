// ---- Mobile nav toggle ----
const menuToggle = document.querySelector(".menu-toggle");
const mobileNav = document.getElementById("mobile-nav");

if (menuToggle && mobileNav) {
  menuToggle.addEventListener("click", () => {
    const isOpen = menuToggle.getAttribute("aria-expanded") === "true";
    menuToggle.setAttribute("aria-expanded", String(!isOpen));
    mobileNav.hidden = isOpen;
  });

  mobileNav.querySelectorAll("a").forEach((link) => {
    link.addEventListener("click", () => {
      menuToggle.setAttribute("aria-expanded", "false");
      mobileNav.hidden = true;
    });
  });
}

// ---- Year in footer ----
const yearEl = document.getElementById("year");
if (yearEl) {
  yearEl.textContent = String(new Date().getFullYear());
}

// ---- Reveal-on-scroll animations ----
const observer = new IntersectionObserver(
  (entries) => {
    entries.forEach((entry) => {
      if (entry.isIntersecting) {
        entry.target.classList.add("is-visible");
        observer.unobserve(entry.target);
      }
    });
  },
  {
    threshold: 0.18,
  }
);

document.querySelectorAll(".reveal").forEach((element) => {
  observer.observe(element);
});

// ---- Language toggle (EN / ES) ----
(function initLanguageToggle() {
  const STORAGE_KEY = "playce-lang";
  const SUPPORTED = ["en", "es"];

  function detectInitialLang() {
    // 1) explicit ?lang=es / ?lang=en in URL wins
    const params = new URLSearchParams(window.location.search);
    const urlLang = params.get("lang");
    if (urlLang && SUPPORTED.includes(urlLang)) return urlLang;

    // 2) saved preference
    const saved = localStorage.getItem(STORAGE_KEY);
    if (saved && SUPPORTED.includes(saved)) return saved;

    // 3) browser language fallback
    const browser = (navigator.language || "en").toLowerCase();
    if (browser.startsWith("es")) return "es";
    return "en";
  }

  function applyLang(lang) {
    if (!SUPPORTED.includes(lang)) lang = "en";
    document.documentElement.lang = lang;
    document.querySelectorAll("[data-set-lang]").forEach((btn) => {
      btn.setAttribute("aria-pressed", String(btn.dataset.setLang === lang));
    });
    try {
      localStorage.setItem(STORAGE_KEY, lang);
    } catch (e) {
      // localStorage may be blocked (incognito); fail silently
    }
  }

  // Initial application
  applyLang(detectInitialLang());

  // Wire up clicks on every lang pill
  document.querySelectorAll("[data-set-lang]").forEach((btn) => {
    btn.addEventListener("click", () => {
      applyLang(btn.dataset.setLang);
    });
  });
})();
