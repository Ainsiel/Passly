#!/usr/bin/env python3
"""generate-report.py — Genera un reporte HTML autocontenido a partir de los summary JSON de k6.

Uso:
  python3 tests/k6/generate-report.py artifacts/k6/ > artifacts/k6/report.html

Lee los archivos *-summary.json del directorio y genera un HTML con CSS inline.
"""
import json
import os
import sys
from pathlib import Path


def load_summary(path):
    with open(path, 'r', encoding='utf-8') as f:
        return json.load(f)


def fmt_ms(val):
    if val is None:
        return 'N/A'
    return f'{val:.1f}'


def fmt_pct(val):
    if val is None:
        return 'N/A'
    return f'{val * 100:.2f}%'


def fmt_rate(val):
    if val is None:
        return 'N/A'
    return f'{val:.1f}'


def check_status(scenario, data, thresholds):
    failures = []
    for metric_name, conditions in thresholds.items():
        metric_data = data.get('metrics', {}).get(metric_name, {})
        values = metric_data.get('values', {})
        for cond in conditions:
            if 'p(95)' in cond and '<' in cond:
                limit = float(cond.split('<')[1])
                actual = values.get('p(95)', 0)
                if actual >= limit:
                    failures.append(f'{metric_name} p95={fmt_ms(actual)}ms >= {fmt_ms(limit)}ms')
            elif 'rate' in cond and '<' in cond:
                limit = float(cond.split('<')[1])
                actual = values.get('rate', 0)
                if actual >= limit:
                    failures.append(f'{metric_name} rate={fmt_pct(actual)} >= {fmt_pct(limit)}')
            elif '>=' in cond:
                parts = cond.split('>=')
                limit = float(parts[1])
                actual = values.get('count', 0) if 'count' in parts[0] else values.get(parts[0], 0)
                if actual < limit:
                    failures.append(f'{metric_name} {parts[0]}={actual} < {limit}')
            elif '==' in cond:
                parts = cond.split('==')
                limit = float(parts[1])
                actual = values.get('count', 0) if 'count' in parts[0] else values.get(parts[0], 0)
                if actual != limit:
                    failures.append(f'{metric_name} {parts[0]}={actual} != {limit}')
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
        'reservations_ok': ['count==50'],
        'reservations_conflict': ['count>=40'],
    },
}


def build_html(results):
    rows = ''
    all_passed = True
    for name, data in results:
        metrics = data.get('metrics', {})
        http_reqs = metrics.get('http_reqs', {}).get('values', {})
        duration = metrics.get('http_req_duration', {}).get('values', {})
        errors = metrics.get('errors', {}).get('values', {})
        ok_count = metrics.get('reservations_ok', {}).get('values', {}).get('count', 0)
        conflict_count = metrics.get('reservations_conflict', {}).get('values', {}).get('count', 0)

        thresholds = THRESHOLDS.get(name, {})
        passed, failures = check_status(name, data, thresholds)
        status_class = 'pass' if passed else 'fail'
        status_text = 'PASS' if passed else 'FAIL'
        if not passed:
            all_passed = False

        rows += f'''
        <tr class="{status_class}">
          <td><strong>{name}</strong></td>
          <td>{fmt_rate(http_reqs.get('rate'))}</td>
          <td>{http_reqs.get('count', 0)}</td>
          <td>{fmt_ms(duration.get('avg'))}</td>
          <td>{fmt_ms(duration.get('p(95)'))}</td>
          <td>{fmt_ms(duration.get('p(99)'))}</td>
          <td>{fmt_pct(errors.get('rate'))}</td>
          <td>{ok_count}</td>
          <td>{conflict_count}</td>
          <td class="status {status_class}">{status_text}</td>
        </tr>'''

    overall_class = 'pass' if all_passed else 'fail'
    overall_text = 'ALL PASS' if all_passed else 'SOME FAILED'

    html = f'''<!DOCTYPE html>
<html lang="en">
<head>
<meta charset="UTF-8">
<meta name="viewport" content="width=device-width, initial-scale=1.0">
<title>Passly k6 Load Test Report</title>
<style>
  * {{ margin: 0; padding: 0; box-sizing: border-box; }}
  body {{ font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, sans-serif; background: #0d1117; color: #c9d1d9; padding: 2rem; }}
  h1 {{ color: #58a6ff; margin-bottom: 0.5rem; font-size: 1.8rem; }}
  .subtitle {{ color: #8b949e; margin-bottom: 2rem; }}
  .overall {{ padding: 1rem 1.5rem; border-radius: 8px; margin-bottom: 2rem; font-size: 1.2rem; font-weight: bold; }}
  .overall.pass {{ background: #0d2818; border: 1px solid #238636; color: #3fb950; }}
  .overall.fail {{ background: #2d1117; border: 1px solid #da3633; color: #f85149; }}
  table {{ width: 100%; border-collapse: collapse; margin-bottom: 2rem; }}
  th {{ background: #161b22; color: #8b949e; text-align: left; padding: 0.75rem 1rem; border-bottom: 2px solid #30363d; font-size: 0.85rem; text-transform: uppercase; }}
  td {{ padding: 0.75rem 1rem; border-bottom: 1px solid #21262d; }}
  tr:hover {{ background: #161b22; }}
  tr.pass td {{ color: #c9d1d9; }}
  tr.fail td {{ color: #f85149; }}
  .status {{ font-weight: bold; text-align: center; }}
  .status.pass {{ color: #3fb950; }}
  .status.fail {{ color: #f85149; }}
  .details {{ background: #161b22; border-radius: 8px; padding: 1.5rem; margin-bottom: 1rem; border: 1px solid #30363d; }}
  .details h3 {{ color: #58a6ff; margin-bottom: 0.5rem; }}
  .details ul {{ list-style: none; padding-left: 0; }}
  .details li {{ padding: 0.25rem 0; color: #8b949e; }}
  .details li strong {{ color: #c9d1d9; }}
  footer {{ color: #484f58; margin-top: 2rem; font-size: 0.85rem; text-align: center; }}
</style>
</head>
<body>
  <h1>Passly k6 Load Test Report</h1>
  <p class="subtitle">QA Ephemeral — benchmark / spike / soak / concurrency</p>

  <div class="overall {overall_class}">{overall_text}</div>

  <table>
    <thead>
      <tr>
        <th>Scenario</th>
        <th>RPS</th>
        <th>Requests</th>
        <th>Avg (ms)</th>
        <th>p95 (ms)</th>
        <th>p99 (ms)</th>
        <th>Errors</th>
        <th>201 OK</th>
        <th>409</th>
        <th>Status</th>
      </tr>
    </thead>
    <tbody>
      {rows}
    </tbody>
  </table>

  <div class="details">
    <h3>Thresholds</h3>
    <ul>
      <li><strong>Benchmark:</strong> ≥100 RPS, p95 &lt; 500ms, error rate &lt; 1%</li>
      <li><strong>Spike:</strong> error rate &lt; 15% during 10x spike</li>
      <li><strong>Soak:</strong> p95 &lt; 500ms, error rate &lt; 1% over 5 min sustained</li>
      <li><strong>Concurrency:</strong> exactly 50 successes (201), ≥40 conflicts (409)</li>
    </ul>
  </div>

  <footer>Generated by Passly QA k6 pipeline</footer>
</body>
</html>'''
    return html


def main():
    if len(sys.argv) < 2:
        print('Usage: generate-report.py <k6-output-dir>', file=sys.stderr)
        sys.exit(1)

    k6_dir = Path(sys.argv[1])
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
    print(html)


if __name__ == '__main__':
    main()
