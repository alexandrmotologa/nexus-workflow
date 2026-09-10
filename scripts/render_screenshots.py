import subprocess
import time
from pathlib import Path

EDGE_PATH = r"C:\Program Files (x86)\Microsoft\Edge\Application\msedge.exe"
OUTPUT_DIR = Path(r"C:\Users\alexander\.gemini\antigravity-ide\scratch\nexus-workflow\docs\images")
OUTPUT_DIR.mkdir(parents=True, exist_ok=True)

def take_screenshot(url, output_file, width=1440, height=880):
    cmd = [
        EDGE_PATH,
        "--headless=new",
        f"--window-size={width},{height}",
        "--run-all-compositor-stages-before-draw",
        "--virtual-time-budget=4000",
        f"--screenshot={output_file.resolve()}",
        url
    ]
    subprocess.run(cmd, check=True)
    print(f"Captured: {output_file.name}")

if __name__ == "__main__":
    # 1. Main Live SVG DAG Visualizer with WAITING_SIGNAL state
    dag_img = OUTPUT_DIR / "nexus-dashboard-dag.png"
    take_screenshot("http://localhost:8080/dashboard?workflow=usr_onboarding_demo", dag_img)

    # 2. Gantt Waterfall Latency Timeline Tab
    waterfall_img = OUTPUT_DIR / "nexus-dashboard-waterfall.png"
    take_screenshot("http://localhost:8080/dashboard?workflow=tier_routing_vip&tab=gantt", waterfall_img)

    # 3. Interactive Operator Intervention Modal
    intervention_img = OUTPUT_DIR / "nexus-dashboard-intervention.png"
    take_screenshot("http://localhost:8080/dashboard?workflow=usr_onboarding_demo&modal=intervention&step=wait-kyc", intervention_img)

    print("All screenshots done!")
