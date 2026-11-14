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
    const params = new URLSearchParams(window.location.search);
    const urlLang = params.get("lang");
    if (urlLang && SUPPORTED.includes(urlLang)) return urlLang;

    try {
      const saved = localStorage.getItem(STORAGE_KEY);
      if (saved && SUPPORTED.includes(saved)) return saved;
    } catch (e) {
      // localStorage may be blocked
    }

    const fallback = document.documentElement.dataset.defaultLang;
    if (fallback && SUPPORTED.includes(fallback)) return fallback;

    const browser = (navigator.language || "en").toLowerCase();
    if (browser.startsWith("es")) return "es";
    return "en";
  }

  function applyLang(lang, persist) {
    if (!SUPPORTED.includes(lang)) lang = document.documentElement.dataset.defaultLang || "en";
    document.documentElement.lang = lang;
    document.querySelectorAll("[data-set-lang]").forEach((btn) => {
      btn.setAttribute("aria-pressed", String(btn.dataset.setLang === lang));
    });
    const title = document.documentElement.getAttribute("data-title-" + lang);
    if (title) document.title = title;
    if (!persist) return;
    try {
      localStorage.setItem(STORAGE_KEY, lang);
    } catch (e) {
      // localStorage may be blocked (incognito); fail silently
    }
  }

  applyLang(detectInitialLang(), false);

  document.querySelectorAll("[data-set-lang]").forEach((btn) => {
    btn.addEventListener("click", () => {
      applyLang(btn.dataset.setLang, true);
    });
  });
})();
