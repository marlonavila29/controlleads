import { DatePipe, DecimalPipe, PercentPipe } from '@angular/common';
import { Component, inject, signal } from '@angular/core';
import { RouterLink } from '@angular/router';
import type { EChartsOption } from 'echarts';
import { ActivitiesService, FollowUpTask } from '../../core/api/activities.service';
import {
  AnalyticsService,
  AnalyticsSummary,
  LeaderboardRow
} from '../../core/api/analytics.service';
import { STATUS_LABELS } from '../../core/api/leads.service';
import { AuthService } from '../../core/auth/auth.service';
import { Chart, baseChartOption, cssVar } from '../../shared/chart';
import { Topbar } from '../../shared/topbar';

@Component({
  selector: 'app-dashboard',
  imports: [Chart, DatePipe, DecimalPipe, PercentPipe, RouterLink, Topbar],
  templateUrl: './dashboard.html',
  styleUrl: './dashboard.scss'
})
export class Dashboard {
  protected readonly auth = inject(AuthService);
  private readonly analytics = inject(AnalyticsService);
  private readonly activities = inject(ActivitiesService);

  protected readonly summary = signal<AnalyticsSummary | null>(null);
  protected readonly funnelChart = signal<EChartsOption | null>(null);
  protected readonly dropOffChart = signal<EChartsOption | null>(null);
  protected readonly timeseriesChart = signal<EChartsOption | null>(null);
  protected readonly channelChart = signal<EChartsOption | null>(null);
  protected readonly countryChart = signal<EChartsOption | null>(null);
  protected readonly leaderboard = signal<LeaderboardRow[]>([]);
  protected readonly followUps = signal<FollowUpTask[]>([]);
  protected readonly offline = signal(false);

  constructor() {
    this.load();
  }

  private load(): void {
    const fail = () => this.offline.set(true);

    this.analytics.summary().subscribe({ next: (s) => this.summary.set(s), error: fail });

    this.analytics.funnel().subscribe({
      next: (stages) => {
        const statusColor: Record<string, string> = {
          LEAD: cssVar('--cl-color-status-lead'),
          HOT_LEAD: cssVar('--cl-color-status-hot-lead'),
          APPLICATION: cssVar('--cl-color-status-application'),
          STUDENT: cssVar('--cl-color-status-student'),
          STALLED: cssVar('--cl-color-status-stalled')
        };
        this.funnelChart.set({
          ...baseChartOption(),
          series: [
            {
              type: 'funnel',
              sort: 'none',
              gap: 4,
              width: '85%',
              label: { formatter: '{b}: {c}' },
              data: stages.map((s) => ({
                name: STATUS_LABELS[s.status],
                value: s.count,
                itemStyle: { color: statusColor[s.status] }
              }))
            }
          ]
        });
      },
      error: fail
    });

    this.analytics.dropOff().subscribe({
      next: (rows) => {
        const byReason = new Map<string, number>();
        for (const row of rows) {
          byReason.set(row.reasonName, (byReason.get(row.reasonName) ?? 0) + row.count);
        }
        const sorted = [...byReason.entries()].sort((a, b) => a[1] - b[1]);
        this.dropOffChart.set({
          ...baseChartOption(),
          grid: { left: 8, right: 24, top: 8, bottom: 8, containLabel: true },
          xAxis: { type: 'value' },
          yAxis: { type: 'category', data: sorted.map(([name]) => name) },
          series: [
            {
              type: 'bar',
              data: sorted.map(([, count]) => count),
              itemStyle: {
                color: cssVar('--cl-color-status-stalled'),
                borderRadius: [0, 6, 6, 0]
              }
            }
          ]
        });
      },
      error: fail
    });

    this.analytics.timeseries('week').subscribe({
      next: (points) => {
        this.timeseriesChart.set({
          ...baseChartOption(),
          tooltip: { trigger: 'axis' },
          legend: { data: ['Created', 'Converted'] },
          grid: { left: 8, right: 24, top: 40, bottom: 8, containLabel: true },
          xAxis: { type: 'category', data: points.map((p) => p.period) },
          yAxis: { type: 'value' },
          series: [
            {
              name: 'Created',
              type: 'line',
              smooth: true,
              areaStyle: { opacity: 0.12 },
              data: points.map((p) => p.created)
            },
            {
              name: 'Converted',
              type: 'line',
              smooth: true,
              areaStyle: { opacity: 0.12 },
              itemStyle: { color: cssVar('--cl-color-status-student') },
              data: points.map((p) => p.converted)
            }
          ]
        });
      },
      error: fail
    });

    this.analytics.byChannel().subscribe({
      next: (rows) => {
        this.channelChart.set({
          ...baseChartOption(),
          tooltip: { trigger: 'axis' },
          legend: { data: ['Leads', 'Students'] },
          grid: { left: 8, right: 24, top: 40, bottom: 8, containLabel: true },
          xAxis: { type: 'category', data: rows.map((r) => r.name), axisLabel: { rotate: 25 } },
          yAxis: { type: 'value' },
          series: [
            { name: 'Leads', type: 'bar', data: rows.map((r) => r.total) },
            {
              name: 'Students',
              type: 'bar',
              itemStyle: { color: cssVar('--cl-color-status-student') },
              data: rows.map((r) => r.students)
            }
          ]
        });
      },
      error: fail
    });

    this.analytics.byCountry().subscribe({
      next: (rows) => {
        const top = rows.slice(0, 12).reverse();
        this.countryChart.set({
          ...baseChartOption(),
          grid: { left: 8, right: 24, top: 8, bottom: 8, containLabel: true },
          xAxis: { type: 'value' },
          yAxis: { type: 'category', data: top.map((r) => r.countryCode) },
          series: [
            {
              type: 'bar',
              data: top.map((r) => r.total),
              itemStyle: { borderRadius: [0, 6, 6, 0] },
              label: { show: true, position: 'right' }
            }
          ]
        });
      },
      error: fail
    });

    if (this.auth.isAdmin()) {
      this.analytics.leaderboard().subscribe({
        next: (rows) => this.leaderboard.set(rows),
        error: fail
      });
    }

    this.activities.myFollowUps().subscribe({
      next: (tasks) => this.followUps.set(tasks),
      error: () => {}
    });
  }

  protected completeTask(task: FollowUpTask): void {
    this.activities.complete(task.id).subscribe(() => {
      this.followUps.update((tasks) => tasks.filter((t) => t.id !== task.id));
    });
  }
}
