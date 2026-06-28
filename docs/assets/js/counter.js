document.addEventListener("DOMContentLoaded", () => {
    const counterKey = "hash2pass_counter_cached_svg";
    const lastVisitKey = "hash2pass_last_visit_time";
    const now = new Date().getTime();
    
    // We update the cache if it's older than 24 hours
    const shouldFetch = !localStorage.getItem(counterKey) || 
                        !localStorage.getItem(lastVisitKey) ||
                        (now - parseInt(localStorage.getItem(lastVisitKey))) > 24 * 60 * 60 * 1000;
                        
    const imgUrl = "https://hits.sh/shashikant70086.github.io/hash2pass.svg?view=today-total&style=for-the-badge&label=visitors&color=1a1d24";
    // Using corsproxy.io to bypass CORS issues when fetching the raw SVG text
    const proxyUrl = "https://corsproxy.io/?" + encodeURIComponent(imgUrl);

    const badgeContainers = document.querySelectorAll('.visitor-badge-container');

    if (shouldFetch) {
        fetch(proxyUrl)
            .then(res => {
                if (!res.ok) throw new Error("Network response was not ok");
                return res.text();
            })
            .then(svg => {
                // Ensure we got an actual SVG and not an error page
                if(svg.includes('<svg')) {
                    localStorage.setItem(counterKey, svg);
                    localStorage.setItem(lastVisitKey, now.toString());
                    badgeContainers.forEach(container => container.innerHTML = svg);
                } else {
                    throw new Error("Invalid SVG format");
                }
            })
            .catch(err => {
                console.error("Failed to load counter", err);
                // Fallback: if fetch fails, just insert the image tag directly.
                // It will increment, but at least it shows up.
                badgeContainers.forEach(container => {
                    container.innerHTML = `<img alt="Hits" src="${imgUrl}" />`;
                });
            });
    } else {
        const cachedSvg = localStorage.getItem(counterKey);
        badgeContainers.forEach(container => {
            if (cachedSvg && cachedSvg.includes('<svg')) {
                container.innerHTML = cachedSvg;
            } else {
                // Fallback
                container.innerHTML = `<img alt="Hits" src="${imgUrl}" />`;
            }
        });
    }
});
