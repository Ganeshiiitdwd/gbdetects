document.addEventListener('DOMContentLoaded', () => {
    const img_el = document.getElementById('frameImage') as HTMLImageElement;
    const stats_el = document.getElementById('statsOverlay') as HTMLDivElement;

    if (img_el && stats_el) {
        // Set the source for our sample image
        img_el.src = './sample_frame.png';

        // Display dummy stats
        const w = 480;
        const h = 640;
        const fps = 15 + Math.floor(Math.random() * 5); // Randomize slightly

        stats_el.innerText = `Resolution: ${w}x${h} | FPS: ${fps} (dummy)`;
    }
});
