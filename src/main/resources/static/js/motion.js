/* TriCity Connect — motion helpers used on every page.
   - scroll reveal with stagger for cards, sections, table rows
   - number counters for stat tiles
   - ripple on button click
   - smooth page-leave transition + top progress bar
   - shadow on sticky header after scrolling */
(function () {
    const reduce = window.matchMedia('(prefers-reduced-motion: reduce)').matches;

    function ready(fn) {
        if (document.readyState !== 'loading') fn(); else document.addEventListener('DOMContentLoaded', fn);
    }

    ready(function () {
        // ---------- progress bar
        const bar = document.createElement('div');
        bar.id = 'nav-progress';
        document.body.appendChild(bar);

        if (reduce) return;

        // ---------- scroll reveal (staggered per parent)
        const targets = document.querySelectorAll('main .card, main section, main article, main tbody tr, main h1, main h2');
        const groups = new Map();
        targets.forEach(el => {
            if (el.closest('.reveal') && el.closest('.reveal') !== el) return; // parent already animates
            if (el.closest('header, footer, nav, .sticky')) return;
            const p = el.parentElement;
            const i = groups.get(p) || 0;
            groups.set(p, i + 1);
            el.classList.add('reveal');
            el.style.setProperty('--d', Math.min(i, 10) * 60 + 'ms');
        });
        const io = new IntersectionObserver(entries => {
            entries.forEach(e => {
                if (e.isIntersecting) {
                    const el = e.target;
                    el.classList.add('in');
                    io.unobserve(el);
                    // once shown, hand control back to normal hover transitions
                    setTimeout(() => { el.classList.remove('reveal', 'in'); el.style.removeProperty('--d'); }, 1300);
                }
            });
        }, {threshold: 0.08, rootMargin: '0px 0px -30px 0px'});
        document.querySelectorAll('.reveal').forEach(el => io.observe(el));

        // ---------- counters: big numbers inside stat tiles
        const counterIo = new IntersectionObserver(entries => {
            entries.forEach(e => {
                if (!e.isIntersecting) return;
                counterIo.unobserve(e.target);
                animateNumber(e.target);
            });
        }, {threshold: 0.4});
        document.querySelectorAll('main .text-3xl, main .text-4xl, main .text-2xl.font-extrabold').forEach(el => {
            if (el.children.length === 0 && /\d/.test(el.textContent) && el.textContent.trim().length < 18) counterIo.observe(el);
        });

        function animateNumber(el) {
            const text = el.textContent.trim();
            const m = text.match(/-?[\d,]+(\.\d+)?/);
            if (!m) return;
            const raw = m[0];
            const decimals = m[1] ? m[1].length - 1 : 0;
            const target = parseFloat(raw.replace(/,/g, ''));
            if (!isFinite(target) || target === 0) return;
            const useCommas = raw.includes(',');
            const before = text.slice(0, m.index), after = text.slice(m.index + raw.length);
            const start = performance.now(), dur = 1100;
            el.classList.add('counting');
            function fmt(v) {
                let s = v.toFixed(decimals);
                if (useCommas) s = Number(s).toLocaleString('en-IN', {minimumFractionDigits: decimals, maximumFractionDigits: decimals});
                return before + s + after;
            }
            (function step(now) {
                const t = Math.min(1, (now - start) / dur);
                const eased = 1 - Math.pow(1 - t, 3);
                el.textContent = fmt(target * eased);
                if (t < 1) requestAnimationFrame(step); else el.textContent = text;
            })(start);
        }

        // ---------- ripple
        document.addEventListener('pointerdown', e => {
            const b = e.target.closest('.btn-primary, .btn-navy, button.bg-green-600, button.bg-navy, button.bg-brand');
            if (!b) return;
            const r = b.getBoundingClientRect();
            const s = Math.max(r.width, r.height);
            const span = document.createElement('span');
            span.className = 'ripple';
            span.style.width = span.style.height = s + 'px';
            span.style.left = (e.clientX - r.left - s / 2) + 'px';
            span.style.top = (e.clientY - r.top - s / 2) + 'px';
            if (getComputedStyle(b).position === 'static') b.style.position = 'relative';
            b.style.overflow = 'hidden';
            b.appendChild(span);
            setTimeout(() => span.remove(), 650);
        });

        // ---------- header shadow on scroll
        const header = document.querySelector('header.sticky');
        if (header) {
            const onScroll = () => header.classList.toggle('scrolled', window.scrollY > 8);
            window.addEventListener('scroll', onScroll, {passive: true});
            onScroll();
        }

        // ---------- leaving the page: fade + progress bar
        function leave() {
            bar.style.opacity = '1';
            bar.style.width = '70%';
            document.body.classList.add('page-leaving');
        }
        document.addEventListener('click', e => {
            const a = e.target.closest('a[href]');
            if (!a || e.defaultPrevented || e.metaKey || e.ctrlKey || e.shiftKey || e.button !== 0) return;
            const href = a.getAttribute('href');
            if (!href || href.startsWith('#') || href.startsWith('javascript') || a.target === '_blank' || a.hasAttribute('download')
                || /\.(pdf|csv)(\?|$)/.test(href) || a.origin !== location.origin) return;
            leave();
        });
        document.addEventListener('submit', e => { if (!e.defaultPrevented) { bar.style.opacity = '1'; bar.style.width = '70%'; } });
        // coming back with the browser Back button restores the page
        window.addEventListener('pageshow', ev => {
            if (ev.persisted) { document.body.classList.remove('page-leaving'); bar.style.width = '0'; }
        });
    });
})();

/* ===================== Motion layer v2 ===================== */
(function () {
    const reduce = window.matchMedia('(prefers-reduced-motion: reduce)').matches;
    const finePointer = window.matchMedia('(hover: hover) and (pointer: fine)').matches;

    function ready(fn) {
        if (document.readyState !== 'loading') fn(); else document.addEventListener('DOMContentLoaded', fn);
    }

    ready(function () {
        // ---------- toast flash messages: close button + auto-dismiss (always on)
        document.querySelectorAll('.bg-green-50.border-green-200, .bg-amber-50.border-amber-200').forEach(box => {
            if (!box.closest('main') && !box.parentElement.classList.contains('max-w-7xl')) return;
            box.classList.add('toast');
            const close = document.createElement('button');
            close.className = 'toast-close';
            close.type = 'button';
            close.setAttribute('aria-label', 'Close');
            close.textContent = '✕';
            box.appendChild(close);
            const hide = () => box.classList.add('hide');
            close.addEventListener('click', hide);
            if (box.classList.contains('bg-green-50')) {   // success messages go away by themselves
                const bar = document.createElement('span');
                bar.className = 'toast-bar';
                box.appendChild(bar);
                setTimeout(hide, 7000);
            }
        });

        // ---------- loading spinner on submit buttons
        document.addEventListener('submit', e => {
            const btn = e.submitter || e.target.querySelector('button[type=submit], button:not([type])');
            if (btn && !e.defaultPrevented && e.target.method.toLowerCase() === 'post') btn.classList.add('is-loading');
        });

        if (reduce) return;

        // ---------- hero: blobs + road with a moving electric bus
        document.querySelectorAll('section.from-navy').forEach(hero => {
            ['b1', 'b2', 'b3'].forEach(c => {
                const b = document.createElement('div');
                b.className = 'blob ' + c;
                hero.prepend(b);
            });
            const road = document.createElement('div');
            road.className = 'road';
            road.innerHTML = '<div class="mini-bus"><svg viewBox="0 0 64 26" width="64" height="26">' +
                '<rect x="1" y="2" width="60" height="18" rx="5" fill="#F97316"/>' +
                '<rect x="5" y="5" width="9" height="7" rx="1.5" fill="#E0F2FE"/><rect x="17" y="5" width="9" height="7" rx="1.5" fill="#E0F2FE"/>' +
                '<rect x="29" y="5" width="9" height="7" rx="1.5" fill="#E0F2FE"/><rect x="41" y="5" width="9" height="7" rx="1.5" fill="#E0F2FE"/>' +
                '<rect x="53" y="5" width="6" height="11" rx="1.5" fill="#E0F2FE"/><rect x="1" y="14" width="60" height="2" fill="#fff" opacity=".7"/>' +
                '<circle cx="14" cy="21" r="4" fill="#0F172A"/><circle cx="14" cy="21" r="1.6" fill="#94A3B8"/>' +
                '<circle cx="48" cy="21" r="4" fill="#0F172A"/><circle cx="48" cy="21" r="1.6" fill="#94A3B8"/>' +
                '<text x="31" y="4" font-size="3.2" fill="#fff" text-anchor="middle">⚡</text></svg></div>';
            hero.appendChild(road);
        });
        document.querySelectorAll('.bg-navy.rounded-2xl').forEach(card => {
            const b = document.createElement('div');
            b.className = 'blob b1';
            b.style.cssText = 'width:180px;height:180px;top:-80px;right:-40px;z-index:-1';
            card.style.position = 'relative';
            card.style.isolation = 'isolate';
            card.prepend(b);
        });

        // ---------- headline: split words so they rise one by one
        document.querySelectorAll('section.from-navy h1, main > div h1.text-4xl').forEach(h => {
            if (h.children.length) return;
            const words = h.textContent.trim().split(/\s+/);
            h.textContent = '';
            words.forEach((w, i) => {
                const s = document.createElement('span');
                s.className = 'word';
                s.style.setProperty('--i', i);
                s.textContent = w;
                h.appendChild(s);
                if (i < words.length - 1) h.appendChild(document.createTextNode(' '));
            });
        });

        // ---------- scroll progress + back-to-top
        const sp = document.createElement('div');
        sp.id = 'scroll-progress';
        document.body.appendChild(sp);
        const top = document.createElement('button');
        top.id = 'to-top';
        top.type = 'button';
        top.setAttribute('aria-label', 'Back to top');
        top.textContent = '↑';
        top.addEventListener('click', () => window.scrollTo({top: 0, behavior: 'smooth'}));
        document.body.appendChild(top);
        let ticking = false;
        function onScroll() {
            const max = document.documentElement.scrollHeight - innerHeight;
            sp.style.transform = 'scaleX(' + (max > 0 ? scrollY / max : 0) + ')';
            top.classList.toggle('show', scrollY > 500);
            ticking = false;
        }
        window.addEventListener('scroll', () => { if (!ticking) { ticking = true; requestAnimationFrame(onScroll); } }, {passive: true});
        onScroll();

        // ---------- images fade in
        document.querySelectorAll('main img, section img').forEach(img => {
            if (img.closest('header')) return;
            if (img.complete && img.naturalWidth) return;
            img.classList.add('fade-img');
            img.addEventListener('load', () => img.classList.add('loaded'), {once: true});
            img.addEventListener('error', () => img.classList.add('loaded'), {once: true});
        });

        if (finePointer) {
            // ---------- 3D tilt + spotlight on clickable / stat cards
            const tiltables = document.querySelectorAll('a.card, main section.grid > .card, main .grid > a, main .grid > .card:not(form)');
            tiltables.forEach(el => {
                if (el.querySelector('form, input, select, textarea') || el.offsetHeight > 420) return;
                el.classList.add('tilt', 'spot');
                el.addEventListener('pointermove', e => {
                    const r = el.getBoundingClientRect();
                    const x = (e.clientX - r.left) / r.width, y = (e.clientY - r.top) / r.height;
                    el.style.setProperty('--mx', x * 100 + '%');
                    el.style.setProperty('--my', y * 100 + '%');
                    el.classList.add('tilting');
                    el.style.transform = 'perspective(800px) rotateX(' + ((0.5 - y) * 7) + 'deg) rotateY(' + ((x - 0.5) * 9) + 'deg) translateY(-4px)';
                });
                el.addEventListener('pointerleave', () => {
                    el.classList.remove('tilting');
                    el.style.transform = '';
                });
            });

            // ---------- spotlight only (bigger cards)
            document.querySelectorAll('main .card').forEach(el => {
                if (el.classList.contains('spot')) return;
                el.classList.add('spot');
                el.addEventListener('pointermove', e => {
                    const r = el.getBoundingClientRect();
                    el.style.setProperty('--mx', (e.clientX - r.left) + 'px');
                    el.style.setProperty('--my', (e.clientY - r.top) + 'px');
                });
            });

            // ---------- magnetic primary buttons
            document.querySelectorAll('.btn-primary, .btn-navy').forEach(b => {
                b.classList.add('magnetic');
                b.addEventListener('pointermove', e => {
                    const r = b.getBoundingClientRect();
                    const dx = e.clientX - (r.left + r.width / 2), dy = e.clientY - (r.top + r.height / 2);
                    b.style.transform = 'translate(' + dx * 0.15 + 'px,' + (dy * 0.25 - 2) + 'px)';
                });
                b.addEventListener('pointerleave', () => { b.style.transform = ''; });
            });
        }

        // ---------- confetti on success (booking, pass, rebook) and on the ticket page
        const celebrate = document.querySelector('.ticket') ||
            Array.from(document.querySelectorAll('.toast.bg-green-50')).find(t => /book|pass|confirm|rebook|top/i.test(t.textContent));
        if (celebrate) confetti();

        function confetti() {
            const c = document.createElement('canvas');
            c.id = 'confetti';
            document.body.appendChild(c);
            const ctx = c.getContext('2d');
            const W = c.width = innerWidth, H = c.height = innerHeight;
            const colors = ['#F97316', '#1E3A8A', '#FDBA74', '#3B82F6', '#22C55E', '#EC4899'];
            const parts = Array.from({length: 140}, () => ({
                x: W / 2 + (Math.random() - 0.5) * 200, y: H * 0.35,
                vx: (Math.random() - 0.5) * 14, vy: -Math.random() * 13 - 4,
                s: Math.random() * 6 + 4, r: Math.random() * 6, vr: (Math.random() - 0.5) * 0.3,
                c: colors[(Math.random() * colors.length) | 0]
            }));
            const t0 = performance.now();
            (function frame(now) {
                ctx.clearRect(0, 0, W, H);
                parts.forEach(p => {
                    p.vy += 0.35; p.vx *= 0.99; p.x += p.vx; p.y += p.vy; p.r += p.vr;
                    ctx.save(); ctx.translate(p.x, p.y); ctx.rotate(p.r);
                    ctx.fillStyle = p.c; ctx.fillRect(-p.s / 2, -p.s / 4, p.s, p.s / 2); ctx.restore();
                });
                if (now - t0 < 3200) requestAnimationFrame(frame); else c.remove();
            })(t0);
        }
    });
})();
