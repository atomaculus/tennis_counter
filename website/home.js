// ============================================================
// PLAYCE landing — interactions + live demo scoring engine
// ============================================================

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

// ---- Reveal-on-scroll ----
const observer = new IntersectionObserver(
  (entries) => {
    entries.forEach((entry) => {
      if (entry.isIntersecting) {
        entry.target.classList.add("is-visible");
        observer.unobserve(entry.target);
      }
    });
  },
  { threshold: 0.16 }
);

document.querySelectorAll(".reveal").forEach((el) => observer.observe(el));

// ---- Language toggle (EN / ES) ----
(function initLanguageToggle() {
  const STORAGE_KEY = "playce-lang";
  const SUPPORTED = ["en", "es"];

  function detectInitialLang() {
    const params = new URLSearchParams(window.location.search);
    const urlLang = params.get("lang");
    if (urlLang && SUPPORTED.includes(urlLang)) return urlLang;

    let saved = null;
    try {
      saved = localStorage.getItem(STORAGE_KEY);
    } catch (e) { /* blocked storage */ }
    if (saved && SUPPORTED.includes(saved)) return saved;

    const browser = (navigator.language || "en").toLowerCase();
    return browser.startsWith("es") ? "es" : "en";
  }

  function applyLang(lang) {
    if (!SUPPORTED.includes(lang)) lang = "en";
    document.documentElement.lang = lang;
    document.querySelectorAll("[data-set-lang]").forEach((btn) => {
      btn.setAttribute("aria-pressed", String(btn.dataset.setLang === lang));
    });
    try {
      localStorage.setItem(STORAGE_KEY, lang);
    } catch (e) { /* blocked storage */ }
  }

  applyLang(detectInitialLang());

  document.querySelectorAll("[data-set-lang]").forEach((btn) => {
    btn.addEventListener("click", () => applyLang(btn.dataset.setLang));
  });
})();

// ============================================================
// Live demo: real tennis scoring engine
// Best of 3 sets, standard deuce/advantage, tiebreak to 7 at 6-6.
// ============================================================
(function initScoreboard() {
  const board = document.getElementById("scoreboard");
  if (!board) return;

  const NAMES = ["NADIA", "DIEGO"];
  const POINT_LABELS = ["0", "15", "30", "40"];
  const SETS_TO_WIN = 2;

  let state = null;
  let history = [];

  function freshState() {
    return {
      points: [0, 0],        // point counter within game (or tiebreak points)
      games: [0, 0],         // games in current set
      sets: [0, 0],          // sets won
      finishedSets: [],      // e.g. [[6,4],[3,6]]
      server: 0,
      tiebreak: false,
      tbStartServer: 0,
      winner: null
    };
  }

  function snapshot() {
    history.push(JSON.parse(JSON.stringify(state)));
    if (history.length > 300) history.shift();
  }

  function winGame(p, viaTiebreak) {
    state.games[p] += 1;
    state.points = [0, 0];

    const g = state.games;
    const setWon =
      viaTiebreak ||
      (g[p] >= 6 && g[p] - g[1 - p] >= 2);

    if (setWon) {
      state.sets[p] += 1;
      state.finishedSets.push([g[0], g[1]]);
      state.games = [0, 0];
      state.tiebreak = false;
      if (state.sets[p] >= SETS_TO_WIN) {
        state.winner = p;
      }
    } else if (g[0] === 6 && g[1] === 6) {
      state.tiebreak = true;
      state.tbStartServer = 1 - state.server;
    }

    // next game: alternate server
    state.server = 1 - state.server;
  }

  function scorePoint(p) {
    if (state.winner !== null) return;
    snapshot();

    if (state.tiebreak) {
      state.points[p] += 1;
      const [a, b] = state.points;
      const mine = state.points[p];
      const theirs = state.points[1 - p];
      // tiebreak serve rotation: first point by tbStartServer, then every 2
      const total = a + b;
      state.server =
        total % 4 === 1 || total % 4 === 2
          ? 1 - state.tbStartServer
          : state.tbStartServer;
      if (mine >= 7 && mine - theirs >= 2) {
        winGame(p, true);
      }
    } else {
      const mine = state.points[p];
      const theirs = state.points[1 - p];
      if (mine >= 3 && theirs >= 3) {
        // deuce territory
        if (mine === theirs) {
          state.points[p] += 1;             // advantage
        } else if (mine > theirs) {
          winGame(p, false);                // had advantage, wins game
        } else {
          state.points[1 - p] -= 1;         // back to deuce
        }
      } else if (mine === 3) {
        winGame(p, false);                  // 40 + point = game
      } else {
        state.points[p] += 1;
      }
    }

    render(p);
  }

  function pointLabel(p) {
    if (state.tiebreak) return String(state.points[p]);
    const mine = state.points[p];
    const theirs = state.points[1 - p];
    if (mine >= 3 && theirs >= 3) {
      if (mine === theirs) return "40";
      return mine > theirs ? "AD" : "40";
    }
    return POINT_LABELS[Math.min(mine, 3)];
  }

  const els = {
    sets: [document.getElementById("sets-0"), document.getElementById("sets-1")],
    games: [document.getElementById("games-0"), document.getElementById("games-1")],
    points: [document.getElementById("points-0"), document.getElementById("points-1")],
    serve: [document.getElementById("serve-0"), document.getElementById("serve-1")],
    finished: document.getElementById("sb-finished-sets"),
    banner: document.getElementById("sb-banner")
  };

  function setText(el, value, animate) {
    if (el.textContent !== value) {
      el.textContent = value;
      if (animate) {
        el.classList.remove("bump");
        void el.offsetWidth; // restart animation
        el.classList.add("bump");
      }
    }
  }

  function render(changedPlayer) {
    const animate = changedPlayer !== undefined;
    for (const p of [0, 1]) {
      setText(els.sets[p], String(state.sets[p]), animate);
      setText(els.games[p], String(state.games[p]), animate);
      setText(els.points[p], pointLabel(p), animate);
      els.serve[p].classList.toggle("is-serving", state.winner === null && state.server === p);
    }

    els.finished.textContent = state.finishedSets.length
      ? state.finishedSets.map(([a, b]) => `${a}-${b}`).join("  ")
      : "—";

    if (state.winner !== null) {
      const es = document.documentElement.lang === "es";
      els.banner.textContent = es
        ? `GAME, SET, MATCH — ${NAMES[state.winner]}`
        : `GAME, SET, MATCH — ${NAMES[state.winner]}`;
      els.banner.hidden = false;
    } else if (state.tiebreak) {
      els.banner.textContent = "TIEBREAK";
      els.banner.hidden = false;
    } else {
      els.banner.hidden = true;
    }
  }

  document.querySelectorAll(".sb-row").forEach((row) => {
    row.addEventListener("click", () => {
      scorePoint(Number(row.dataset.player));
    });
  });

  document.getElementById("sb-undo").addEventListener("click", () => {
    const prev = history.pop();
    if (prev) {
      state = prev;
      render(0);
    }
  });

  document.getElementById("sb-reset").addEventListener("click", () => {
    state = freshState();
    history = [];
    render();
  });

  state = freshState();
  render();
})();
