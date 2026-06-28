document.addEventListener('DOMContentLoaded', () => {
    const nav = document.querySelector('.liquid-nav');
    window.addEventListener('scroll', () => {
        if (window.scrollY > 50) {
            nav.classList.add('scrolled');
        } else {
            nav.classList.remove('scrolled');
        }
    }, { passive: true });

    let mouseX = window.innerWidth / 2;
    let mouseY = window.innerHeight / 2;
    let mouseMoved = false;

    document.addEventListener("mousemove", (e) => {
        mouseX = e.clientX;
        mouseY = e.clientY;
        mouseMoved = true;
    }, { passive: true });

    const hoverOverlay = document.getElementById('hover-overlay');
    const hoverCells = Array.from(document.querySelectorAll('.hover-cell'));
    
    const CANVAS = document.getElementById('grid-canvas');
    const ctx = CANVAS.getContext('2d');
    const CANVAS_FG = document.getElementById('grid-canvas-fg');
    const ctxFg = CANVAS_FG.getContext('2d');
    const COLORS = ['#377B2B', '#F47A1F', '#00529B', '#01204E', '#028292', '#F5DBAB', '#F9A968', '#F55625'];
    const TAP_THRESHOLD = 8;
    
    let cellSize = 64;
    let cols = 0;
    let rows = 0;
    let resizeTimer = null;
    let activeFills = [];
    
    let touchStartX = 0;
    let touchStartY = 0;
    let touchHasMoved = false;

    function getCellSize() {
        const w = window.innerWidth;
        if (w < 480) return 32;
        if (w < 768) return 48;
        return 64;
    }

    function resizeAndDraw() {
        cellSize = getCellSize();
        const w = CANVAS.clientWidth;
        const h = CANVAS.clientHeight;
        
        cols = Math.ceil(w / cellSize) + 1;
        rows = Math.ceil(h / cellSize) + 1;
        
        CANVAS.width = w * window.devicePixelRatio;
        CANVAS.height = h * window.devicePixelRatio;
        ctx.scale(window.devicePixelRatio, window.devicePixelRatio);

        CANVAS_FG.width = w * window.devicePixelRatio;
        CANVAS_FG.height = h * window.devicePixelRatio;
        ctxFg.scale(window.devicePixelRatio, window.devicePixelRatio);
        
        hoverOverlay.style.width = `${cellSize * 3}px`;
        hoverOverlay.style.height = `${cellSize * 3}px`;
        
        drawCanvas();
    }

    function drawCanvas() {
        const w = CANVAS.clientWidth;
        const h = CANVAS.clientHeight;
        ctx.clearRect(0, 0, w, h);
        ctxFg.clearRect(0, 0, w, h);
        
        const now = performance.now();
        activeFills = activeFills.filter(f => now - f.startTime < 3000);
        
        for (const fill of activeFills) {
            ctxFg.fillStyle = fill.color;
            ctxFg.fillRect(fill.col * cellSize, fill.row * cellSize, cellSize, cellSize);
        }
        
        ctx.beginPath();
        ctx.strokeStyle = '#D1D5DB';
        ctx.lineWidth = 1;
        
        for (let c = 1; c <= cols; c++) {
            const x = c * cellSize;
            ctx.moveTo(x, 0);
            ctx.lineTo(x, h);
        }
        for (let r = 1; r <= rows; r++) {
            const y = r * cellSize;
            ctx.moveTo(0, y);
            ctx.lineTo(w, y);
        }
        ctx.stroke();
        
        if (activeFills.length > 0) {
            requestAnimationFrame(drawCanvas);
        }
    }

    window.addEventListener('resize', () => {
        clearTimeout(resizeTimer);
        resizeTimer = setTimeout(resizeAndDraw, 150);
    }, { passive: true });
    
    resizeAndDraw();
    
    let isHovering = false;
    let isMouseInsideDocument = true;

    function updateLoop() {
        if (mouseMoved && isMouseInsideDocument) {
            document.documentElement.style.setProperty("--mouse-x", mouseX + "px");
            document.documentElement.style.setProperty("--mouse-y", mouseY + "px");
            
            const col = Math.floor(mouseX / cellSize);
            const row = Math.floor(mouseY / cellSize);
            
            if (col >= 0 && col < cols && row >= 0 && row < rows && isHovering) {
                hoverOverlay.style.display = 'grid';
                
                const overlayX = (col - 1) * cellSize;
                const overlayY = (row - 1) * cellSize;
                hoverOverlay.style.transform = `translate3d(${overlayX}px, ${overlayY}px, 0)`;
                
                const dx = mouseX - (col * cellSize + cellSize / 2);
                const dy = mouseY - (row * cellSize + cellSize / 2);
                const tiltX = (dy / (cellSize/2)) * 5;
                const tiltY = (dx / (cellSize/2)) * -5;
                const clampedTiltX = Math.max(-5, Math.min(5, tiltX));
                const clampedTiltY = Math.max(-5, Math.min(5, tiltY));

                for (let i = 0; i < 9; i++) {
                    const cOffset = i % 3;
                    const rOffset = Math.floor(i / 3);
                    
                    const cellGlobalLeft = overlayX + cOffset * cellSize;
                    const cellGlobalTop = overlayY + rOffset * cellSize;
                    
                    const localMx = mouseX - cellGlobalLeft;
                    const localMy = mouseY - cellGlobalTop;
                    
                    const mxPct = (localMx / cellSize) * 100 + '%';
                    const myPct = (localMy / cellSize) * 100 + '%';
                    
                    const cell = hoverCells[i];
                    cell.style.setProperty('--mx', mxPct);
                    cell.style.setProperty('--my', myPct);
                    
                    let strength = 0;
                    if (cOffset === 1 && rOffset === 1) strength = 1.0;
                    else if (cOffset === 1 || rOffset === 1) strength = 0.6;
                    else strength = 0.35;
                    
                    cell.style.setProperty('--strength', strength);
                    
                    if (cOffset === 1 && rOffset === 1) {
                        cell.style.transform = `perspective(600px) rotateX(${clampedTiltX}deg) rotateY(${clampedTiltY}deg)`;
                    } else {
                        cell.style.transform = '';
                    }
                }
            } else {
                hoverOverlay.style.display = 'none';
            }
            mouseMoved = false;
        } else if (!isMouseInsideDocument) {
            hoverOverlay.style.display = 'none';
        }
        requestAnimationFrame(updateLoop);
    }
    requestAnimationFrame(updateLoop);
    
    document.addEventListener('mouseenter', () => { isHovering = true; isMouseInsideDocument = true; mouseMoved = true; }, { passive: true });
    document.addEventListener('mouseleave', () => { isHovering = false; isMouseInsideDocument = false; mouseMoved = true; }, { passive: true });

    function handleClick(clientX, clientY) {
        const col = Math.floor(clientX / cellSize);
        const row = Math.floor(clientY / cellSize);
        if (col < 0 || col >= cols || row < 0 || row >= rows) return;
        
        const color = COLORS[Math.floor(Math.random() * COLORS.length)];
        activeFills.push({ col, row, color, startTime: performance.now() });
        
        if (activeFills.length === 1) {
            drawCanvas();
        }
    }

    document.addEventListener('click', e => {
        if (e.target.closest('a, button, input, header')) return;
        handleClick(e.clientX, e.clientY);
    });

    document.addEventListener('touchstart', e => {
        if (e.target.closest('a, button, input, header')) return;
        const t = e.touches[0];
        touchStartX = t.clientX;
        touchStartY = t.clientY;
        touchHasMoved = false;
        
        mouseX = t.clientX;
        mouseY = t.clientY;
        isHovering = true;
        isMouseInsideDocument = true;
        mouseMoved = true;
    }, { passive: true });

    document.addEventListener('touchmove', e => {
        if (e.target.closest('a, button, input, header')) return;
        const t = e.touches[0];
        const dx = Math.abs(t.clientX - touchStartX);
        const dy = Math.abs(t.clientY - touchStartY);
        if (dx > TAP_THRESHOLD || dy > TAP_THRESHOLD) touchHasMoved = true;
        
        mouseX = t.clientX;
        mouseY = t.clientY;
        mouseMoved = true;
    }, { passive: true });

    document.addEventListener('touchend', e => {
        if (!touchHasMoved && !e.target.closest('a, button, input, header')) {
            handleClick(touchStartX, touchStartY);
        }
        isHovering = false;
        isMouseInsideDocument = false;
        mouseMoved = true;
    }, { passive: true });
});
