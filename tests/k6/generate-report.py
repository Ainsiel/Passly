#!/usr/bin/env python3
"""generate-report.py — Genera un reporte HTML autocontenido con graficos a partir de los summary JSON de k6.

Uso:
  python tests/k6/generate-report.py artifacts/k6/ [output-file]

Lee los archivos *-summary.json del directorio y genera un HTML autocontenido
con CSS inline, Chart.js desde CDN y graficos interactivos.
"""
import json
import sys
from pathlib import Path


def load_summary(path):
    with open(path, 'r', encoding='utf-8') as f:
        return json.load(f)


def fmt(val, decimals=1):
    if val is None:
        return 'N/A'
    return f'{val:.{decimals}f}'


def check_status(scenario, data, thresholds):
    failures = []
    for metric_name, conditions in thresholds.items():
        metric_data = data.get('metrics', {}).get(metric_name, {})
        for cond in conditions:
            if 'p(95)' in cond and '<' in cond:
                limit = float(cond.split('<')[1])
                actual = metric_data.get('p(95)', 0)
                if actual >= limit:
                    failures.append(f'p95 {fmt(actual)}ms &ge; {fmt(limit)}ms')
            elif 'rate' in cond and '<' in cond:
                limit = float(cond.split('<')[1])
                actual = metric_data.get('value', metric_data.get('rate', 0))
                if actual >= limit:
                    failures.append(f'error rate {actual*100:.2f}% &ge; {limit*100:.0f}%')
            elif '>=' in cond:
                parts = cond.split('>=')
                limit = float(parts[1])
                actual = metric_data.get('count', metric_data.get('value', 0))
                if actual < limit:
                    failures.append(f'{parts[0]} {int(actual)} &lt; {int(limit)}')
            elif '==' in cond:
                parts = cond.split('==')
                limit = float(parts[1])
                actual = metric_data.get('count', metric_data.get('value', 0))
                if actual != limit:
                    failures.append(f'{parts[0]} {int(actual)} != {int(limit)}')
    return len(failures) == 0, failures


THRESHOLDS = {
    'benchmark': {
        'http_req_duration': ['p(95)<500'],
        'errors': ['rate<0.01'],
        'http_reqs': ['rate>=100'],
    },
    'spike': {
        'errors': ['rate<0.15'],
    },
    'soak': {
        'http_req_duration': ['p(95)<500'],
        'errors': ['rate<0.01'],
    },
    'concurrency': {
        'reservations_ok': ['count>=20'],
        'reservations_conflict': ['count>=50'],
    },
}

SCENARIO_LABELS = {
    'benchmark': 'Benchmark',
    'spike': 'Spike 10x',
    'soak': 'Soak 5min',
    'concurrency': 'Concurrency',
}

SCENARIO_COLORS = {
    'benchmark': '#58a6ff',
    'spike': '#f0883e',
    'soak': '#a371f7',
    'concurrency': '#3fb950',
}


def extract_metrics(name, data):
    m = data.get('metrics', {})
    reqs = m.get('http_reqs', {})
    dur = m.get('http_req_duration', {})
    errs = m.get('errors', {})
    ok = m.get('reservations_ok', {})
    conflict = m.get('reservations_conflict', {})
    return {
        'rps': reqs.get('rate', 0),
        'total': reqs.get('count', 0),
        'avg_ms': dur.get('avg', 0),
        'p90_ms': dur.get('p(90)', 0),
        'p95_ms': dur.get('p(95)', 0),
        'max_ms': dur.get('max', 0),
        'error_rate': errs.get('value', 0),
        'ok_count': ok.get('count', 0),
        'conflict_count': conflict.get('count', 0),
    }


def build_html(results):
    scenarios = []
    all_passed = True
    for name, data in results:
        thresholds = THRESHOLDS.get(name, {})
        passed, failures = check_status(name, data, thresholds)
        if not passed:
            all_passed = False
        metrics = extract_metrics(name, data)
        scenarios.append({
            'name': name,
            'label': SCENARIO_LABELS.get(name, name),
            'color': SCENARIO_COLORS.get(name, '#58a6ff'),
            'passed': passed,
            'failures': failures,
            'metrics': metrics,
        })

    overall_class = 'pass' if all_passed else 'fail'
    overall_icon = '&#10003;' if all_passed else '&#10007;'
    overall_text = 'ALL PASS' if all_passed else 'SOME FAILED'

    scenario_cards = ''
    scenario_details = ''
    for s in scenarios:
        sc = 'pass' if s['passed'] else 'fail'
        m = s['metrics']
        badge = 'PASS' if s['passed'] else 'FAIL'
        fail_html = ''
        if s['failures']:
            items = ''.join(f'<li>{f}</li>' for f in s['failures'])
            fail_html = f'<ul class="fail-list">{items}</ul>'

        scenario_cards += f'''
        <div class="card {sc}">
          <div class="card-header">
            <span class="card-dot" style="background:{s['color']}"></span>
            <span class="card-title">{s['label']}</span>
            <span class="badge badge-{sc}">{badge}</span>
          </div>
          <div class="card-metrics">
            <div class="metric">
              <span class="metric-value">{fmt(m['rps'])}</span>
              <span class="metric-label">RPS</span>
            </div>
            <div class="metric">
              <span class="metric-value">{m['total']:,}</span>
              <span class="metric-label">Requests</span>
            </div>
            <div class="metric">
              <span class="metric-value">{fmt(m['p95_ms'])}ms</span>
              <span class="metric-label">p95</span>
            </div>
            <div class="metric">
              <span class="metric-value">{m['ok_count']}</span>
              <span class="metric-label">201 OK</span>
            </div>
            <div class="metric">
              <span class="metric-value">{m['conflict_count']}</span>
              <span class="metric-label">409</span>
            </div>
          </div>
          {fail_html}
        </div>'''

        latency_bar_width = min(m['p95_ms'] / 500 * 100, 100) if m['p95_ms'] > 0 else 0
        scenario_details += f'''
        <div class="detail-section">
          <h3><span class="card-dot" style="background:{s['color']}"></span> {s['label']}</h3>
          <div class="detail-grid">
            <div class="detail-item">
              <span class="detail-label">Throughput</span>
              <span class="detail-value">{fmt(m['rps'])} req/s</span>
            </div>
            <div class="detail-item">
              <span class="detail-label">Total Requests</span>
              <span class="detail-value">{m['total']:,}</span>
            </div>
            <div class="detail-item">
              <span class="detail-label">Avg Latency</span>
              <span class="detail-value">{fmt(m['avg_ms'])}ms</span>
            </div>
            <div class="detail-item">
              <span class="detail-label">p90 Latency</span>
              <span class="detail-value">{fmt(m['p90_ms'])}ms</span>
            </div>
            <div class="detail-item">
              <span class="detail-label">p95 Latency</span>
              <span class="detail-value">{fmt(m['p95_ms'])}ms</span>
            </div>
            <div class="detail-item">
              <span class="detail-label">Max Latency</span>
              <span class="detail-value">{fmt(m['max_ms'])}ms</span>
            </div>
            <div class="detail-item">
              <span class="detail-label">Error Rate</span>
              <span class="detail-value">{m['error_rate']*100:.2f}%</span>
            </div>
            <div class="detail-item">
              <span class="detail-label">Success (201)</span>
              <span class="detail-value">{m['ok_count']}</span>
            </div>
            <div class="detail-item">
              <span class="detail-label">Conflict (409)</span>
              <span class="detail-value">{m['conflict_count']}</span>
            </div>
          </div>
          <div class="latency-bar-container">
            <span class="latency-bar-label">p95 vs 500ms target</span>
            <div class="latency-bar-track">
              <div class="latency-bar-fill" style="width:{latency_bar_width}%; background:{s['color']}"></div>
            </div>
            <span class="latency-bar-value">{fmt(m['p95_ms'])}ms</span>
          </div>
        </div>'''

    labels_json = json.dumps([s['label'] for s in scenarios])
    rps_json = json.dumps([round(s['metrics']['rps'], 1) for s in scenarios])
    colors_json = json.dumps([s['color'] for s in scenarios])
    p95_json = json.dumps([round(s['metrics']['p95_ms'], 1) for s in scenarios])
    ok_json = json.dumps([s['metrics']['ok_count'] for s in scenarios])
    conflict_json = json.dumps([s['metrics']['conflict_count'] for s in scenarios])

    html = f'''<!DOCTYPE html>
<html lang="en">
<head>
<meta charset="UTF-8">
<meta name="viewport" content="width=device-width, initial-scale=1.0">
<title>Passly k6 Load Test Report</title>
<script src="https://cdn.jsdelivr.net/npm/chart.js@4.4.7/dist/chart.umd.min.js"></script>
<style>
  :root {{
    --bg: #0d1117; --surface: #161b22; --border: #30363d; --text: #c9d1d9;
    --muted: #8b949e; --accent: #58a6ff; --green: #3fb950; --red: #f85149;
    --orange: #f0883e; --purple: #a371f7;
  }}
  * {{ margin: 0; padding: 0; box-sizing: border-box; }}
  body {{ font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, sans-serif;
         background: var(--bg); color: var(--text); padding: 2rem; line-height: 1.6; }}

  .header {{ margin-bottom: 2.5rem; }}
  .header h1 {{ font-size: 2rem; font-weight: 700; margin-bottom: 0.25rem; }}
  .header h1 span {{ color: var(--accent); }}
  .header .subtitle {{ color: var(--muted); font-size: 0.95rem; }}

  .overall {{ display: inline-flex; align-items: center; gap: 0.75rem;
              padding: 0.75rem 1.5rem; border-radius: 8px; font-weight: 700;
              font-size: 1.1rem; margin-bottom: 2rem; }}
  .overall.pass {{ background: #0d2818; border: 1px solid #238636; color: var(--green); }}
  .overall.fail {{ background: #2d1117; border: 1px solid #da3633; color: var(--red); }}
  .overall .icon {{ font-size: 1.4rem; }}

  .section-title {{ font-size: 1.1rem; font-weight: 600; margin-bottom: 1rem;
                     padding-bottom: 0.5rem; border-bottom: 1px solid var(--border); }}

  .cards {{ display: grid; grid-template-columns: repeat(auto-fit, minmax(280px, 1fr));
            gap: 1rem; margin-bottom: 2.5rem; }}
  .card {{ background: var(--surface); border: 1px solid var(--border); border-radius: 12px;
           padding: 1.25rem; transition: border-color 0.2s; }}
  .card:hover {{ border-color: #484f58; }}
  .card.pass {{ border-left: 3px solid var(--green); }}
  .card.fail {{ border-left: 3px solid var(--red); }}
  .card-header {{ display: flex; align-items: center; gap: 0.5rem; margin-bottom: 1rem; }}
  .card-dot {{ width: 10px; height: 10px; border-radius: 50%; flex-shrink: 0; }}
  .card-title {{ font-weight: 600; font-size: 1rem; flex: 1; }}
  .badge {{ padding: 0.15rem 0.6rem; border-radius: 999px; font-size: 0.7rem;
            font-weight: 700; text-transform: uppercase; letter-spacing: 0.05em; }}
  .badge-pass {{ background: #0d2818; color: var(--green); }}
  .badge-fail {{ background: #2d1117; color: var(--red); }}
  .card-metrics {{ display: grid; grid-template-columns: repeat(3, 1fr); gap: 0.75rem; }}
  .metric {{ text-align: center; }}
  .metric-value {{ display: block; font-size: 1.2rem; font-weight: 700; color: var(--text); }}
  .metric-label {{ display: block; font-size: 0.7rem; color: var(--muted); text-transform: uppercase;
                   letter-spacing: 0.05em; margin-top: 0.15rem; }}
  .fail-list {{ margin-top: 0.75rem; padding: 0.5rem 0.75rem; background: #2d1117;
                border-radius: 6px; list-style: none; }}
  .fail-list li {{ color: var(--red); font-size: 0.8rem; padding: 0.15rem 0; }}

  .charts {{ display: grid; grid-template-columns: repeat(auto-fit, minmax(400px, 1fr));
             gap: 1.5rem; margin-bottom: 2.5rem; }}
  .chart-box {{ background: var(--surface); border: 1px solid var(--border); border-radius: 12px;
                padding: 1.5rem; }}
  .chart-box h4 {{ color: var(--muted); font-size: 0.85rem; text-transform: uppercase;
                   letter-spacing: 0.05em; margin-bottom: 1rem; }}
  .chart-canvas {{ width: 100% !important; max-height: 280px; }}

  .detail-section {{ background: var(--surface); border: 1px solid var(--border); border-radius: 12px;
                     padding: 1.5rem; margin-bottom: 1rem; }}
  .detail-section h3 {{ display: flex; align-items: center; gap: 0.5rem;
                        font-size: 1rem; margin-bottom: 1rem; }}
  .detail-grid {{ display: grid; grid-template-columns: repeat(auto-fit, minmax(140px, 1fr));
                  gap: 0.75rem; margin-bottom: 1rem; }}
  .detail-item {{ padding: 0.5rem; background: var(--bg); border-radius: 6px; }}
  .detail-label {{ display: block; font-size: 0.7rem; color: var(--muted); text-transform: uppercase;
                   letter-spacing: 0.05em; margin-bottom: 0.25rem; }}
  .detail-value {{ display: block; font-size: 0.95rem; font-weight: 600; }}
  .latency-bar-container {{ display: flex; align-items: center; gap: 0.75rem;
                            margin-top: 0.75rem; padding-top: 0.75rem; border-top: 1px solid var(--border); }}
  .latency-bar-label {{ font-size: 0.75rem; color: var(--muted); white-space: nowrap; }}
  .latency-bar-track {{ flex: 1; height: 8px; background: var(--bg); border-radius: 4px; overflow: hidden; }}
  .latency-bar-fill {{ height: 100%; border-radius: 4px; transition: width 0.5s ease; }}
  .latency-bar-value {{ font-size: 0.8rem; font-weight: 600; min-width: 60px; text-align: right; }}

  .thresholds {{ background: var(--surface); border: 1px solid var(--border); border-radius: 12px;
                 padding: 1.5rem; margin-bottom: 2rem; }}
  .thresholds h3 {{ font-size: 1rem; margin-bottom: 1rem; }}
  .threshold-grid {{ display: grid; grid-template-columns: repeat(auto-fit, minmax(250px, 1fr));
                     gap: 0.75rem; }}
  .threshold-item {{ padding: 0.75rem; background: var(--bg); border-radius: 6px;
                     font-size: 0.85rem; }}
  .threshold-item strong {{ color: var(--accent); }}

  footer {{ color: #484f58; text-align: center; font-size: 0.8rem; margin-top: 2rem;
            padding-top: 1rem; border-top: 1px solid var(--border); }}

  @media (max-width: 768px) {{
    body {{ padding: 1rem; }}
    .charts {{ grid-template-columns: 1fr; }}
    .card-metrics {{ grid-template-columns: repeat(2, 1fr); }}
  }}
</style>
</head>
<body>

<div class="header">
  <h1><span>Passly</span> k6 Load Test Report</h1>
  <p class="subtitle">QA Ephemeral &mdash; benchmark / spike / soak / concurrency</p>
</div>

<div class="overall {overall_class}">
  <span class="icon">{overall_icon}</span>
  {overall_text}
</div>

<h2 class="section-title">Scenarios Overview</h2>
<div class="cards">
  {scenario_cards}
</div>

<h2 class="section-title">Charts</h2>
<div class="charts">
  <div class="chart-box">
    <h4>Throughput (RPS)</h4>
    <canvas id="chartRps" class="chart-canvas"></canvas>
  </div>
  <div class="chart-box">
    <h4>Latency p95 (ms)</h4>
    <canvas id="chartP95" class="chart-canvas"></canvas>
  </div>
  <div class="chart-box">
    <h4>Reservations Breakdown</h4>
    <canvas id="chartReservations" class="chart-canvas"></canvas>
  </div>
  <div class="chart-box">
    <h4>Success vs Conflict Ratio</h4>
    <canvas id="chartRatio" class="chart-canvas"></canvas>
  </div>
</div>

<h2 class="section-title">Detailed Results</h2>
{scenario_details}

<div class="thresholds">
  <h3>Acceptance Thresholds</h3>
  <div class="threshold-grid">
    <div class="threshold-item"><strong>Benchmark:</strong> &ge;100 RPS, p95 &lt; 500ms, error &lt; 1%</div>
    <div class="threshold-item"><strong>Spike:</strong> error rate &lt; 15% during 10x traffic spike</div>
    <div class="threshold-item"><strong>Soak:</strong> p95 &lt; 500ms, error &lt; 1% over 5 min sustained</div>
    <div class="threshold-item"><strong>Concurrency:</strong> &ge;20 successes (201), &ge;50 conflicts (409)</div>
  </div>
</div>

<footer>Generated by Passly QA k6 pipeline</footer>

<script>
const labels = {labels_json};
const rpsData = {rps_json};
const p95Data = {p95_json};
const colors = {colors_json};
const okData = {ok_json};
const conflictData = {conflict_json};

const chartDefaults = {{
  responsive: true,
  maintainAspectRatio: false,
  plugins: {{
    legend: {{ display: false }},
  }},
  scales: {{
    x: {{
      grid: {{ color: '#21262d' }},
      ticks: {{ color: '#8b949e', font: {{ size: 11 }} }},
    }},
    y: {{
      grid: {{ color: '#21262d' }},
      ticks: {{ color: '#8b949e', font: {{ size: 11 }} }},
      beginAtZero: true,
    }},
  }},
}};

new Chart(document.getElementById('chartRps'), {{
  type: 'bar',
  data: {{
    labels: labels,
    datasets: [{{
      data: rpsData,
      backgroundColor: colors.map(c => c + 'cc'),
      borderColor: colors,
      borderWidth: 2,
      borderRadius: 6,
    }}],
  }},
  options: {{
    ...chartDefaults,
    plugins: {{
      ...chartDefaults.plugins,
      tooltip: {{
        callbacks: {{
          label: (ctx) => ctx.parsed.y + ' req/s',
        }},
      }},
    }},
  }},
}});

new Chart(document.getElementById('chartP95'), {{
  type: 'bar',
  data: {{
    labels: labels,
    datasets: [{{
      data: p95Data,
      backgroundColor: colors.map(c => c + 'cc'),
      borderColor: colors,
      borderWidth: 2,
      borderRadius: 6,
    }}],
  }},
  options: {{
    ...chartDefaults,
    plugins: {{
      ...chartDefaults.plugins,
      annotation: {{
        annotations: {{
          target: {{
            type: 'line',
            yMin: 500,
            yMax: 500,
            borderColor: '#f8514966',
            borderWidth: 2,
            borderDash: [6, 4],
            label: {{
              content: 'Target: 500ms',
              display: true,
              position: 'end',
              color: '#f85149',
              font: {{ size: 10 }},
            }},
          }},
        }},
      }},
      tooltip: {{
        callbacks: {{
          label: (ctx) => ctx.parsed.y + 'ms',
        }},
      }},
    }},
    scales: {{
      ...chartDefaults.scales,
      y: {{
        ...chartDefaults.scales.y,
        suggestedMax: Math.max(...p95Data) * 1.3,
      }},
    }},
  }},
}});

new Chart(document.getElementById('chartReservations'), {{
  type: 'bar',
  data: {{
    labels: labels,
    datasets: [
      {{
        label: 'Success (201)',
        data: okData,
        backgroundColor: '#3fb950cc',
        borderColor: '#3fb950',
        borderWidth: 2,
        borderRadius: 6,
      }},
      {{
        label: 'Conflict (409)',
        data: conflictData,
        backgroundColor: '#f0883ecc',
        borderColor: '#f0883e',
        borderWidth: 2,
        borderRadius: 6,
      }},
    ],
  }},
  options: {{
    ...chartDefaults,
    plugins: {{
      legend: {{
        display: true,
        position: 'top',
        labels: {{ color: '#8b949e', font: {{ size: 11 }}, boxWidth: 12, padding: 16 }},
      }},
    }},
    scales: {{
      ...chartDefaults.scales,
      x: {{ ...chartDefaults.scales.x, stacked: true }},
      y: {{ ...chartDefaults.scales.y, stacked: true }},
    }},
  }},
}});

const ratioData = okData.map((ok, i) => {{
  const total = ok + conflictData[i];
  return total > 0 ? Math.round(ok / total * 100) : 0;
}});

new Chart(document.getElementById('chartRatio'), {{
  type: 'doughnut',
  data: {{
    labels: labels,
    datasets: [{{
      data: ratioData,
      backgroundColor: colors.map(c => c + 'cc'),
      borderColor: colors,
      borderWidth: 2,
      hoverOffset: 8,
    }}],
  }},
  options: {{
    responsive: true,
    maintainAspectRatio: false,
    cutout: '55%',
    plugins: {{
      legend: {{
        display: true,
        position: 'right',
        labels: {{ color: '#8b949e', font: {{ size: 11 }}, padding: 12, boxWidth: 12 }},
      }},
      tooltip: {{
        callbacks: {{
          label: (ctx) => ctx.label + ': ' + ctx.parsed + '% success',
        }},
      }},
    }},
  }},
}});
</script>

</body>
</html>'''
    return html


def main():
    if len(sys.argv) < 2:
        print('Usage: generate-report.py <k6-output-dir> [output-file]', file=sys.stderr)
        sys.exit(1)

    k6_dir = Path(sys.argv[1])
    output_file = Path(sys.argv[2]) if len(sys.argv) > 2 else None
    results = []

    for scenario in ['benchmark', 'spike', 'soak', 'concurrency']:
        summary_file = k6_dir / f'{scenario}-summary.json'
        if summary_file.exists():
            data = load_summary(summary_file)
            results.append((scenario, data))
        else:
            print(f'Warning: {summary_file} not found, skipping', file=sys.stderr)

    if not results:
        print('Error: no summary files found', file=sys.stderr)
        sys.exit(1)

    html = build_html(results)

    if output_file:
        output_file.write_text(html, encoding='utf-8')
        print(f'Report written to {output_file}', file=sys.stderr)
    else:
        sys.stdout.buffer.write(html.encode('utf-8'))


if __name__ == '__main__':
    main()
