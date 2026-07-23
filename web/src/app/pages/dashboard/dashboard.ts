import { DatePipe, DecimalPipe, PercentPipe } from '@angular/common';
import { Component, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { RouterLink } from '@angular/router';
import type { EChartsOption } from 'echarts';
import { ActivitiesService, FollowUpTask } from '../../core/api/activities.service';
import {
  AnalyticsService,
  AnalyticsSummary,
  DateRange,
  LeaderboardRow
} from '../../core/api/analytics.service';
import { STATUS_LABELS } from '../../core/api/leads.service';
import { AuthService } from '../../core/auth/auth.service';
import { Chart, baseChartOption, cssVar } from '../../shared/chart';
import { Sidebar } from '../../shared/sidebar';
import { CatalogService } from '../../core/api/catalog.service';
import { UsersService } from '../../core/api/users.service';

@Component({
  selector: 'app-dashboard',
  imports: [Chart, DatePipe, DecimalPipe, FormsModule, PercentPipe, RouterLink, Sidebar],
  templateUrl: './dashboard.html',
  styleUrl: './dashboard.scss'
})
export class Dashboard {
  protected readonly auth = inject(AuthService);
  private readonly analytics = inject(AnalyticsService);
  private readonly activities = inject(ActivitiesService);
  private readonly usersService = inject(UsersService);
  private readonly catalogService = inject(CatalogService);

  protected readonly summary = signal<AnalyticsSummary | null>(null);
  protected readonly funnelChart = signal<EChartsOption | null>(null);
  protected readonly dropOffChart = signal<EChartsOption | null>(null);
  protected readonly timeseriesChart = signal<EChartsOption | null>(null);
  protected readonly channelChart = signal<EChartsOption | null>(null);
  protected readonly countryChart = signal<EChartsOption | null>(null);
  protected readonly leaderboard = signal<LeaderboardRow[]>([]);
  protected readonly followUps = signal<FollowUpTask[]>([]);
  protected readonly offline = signal(false);

  // Counselor options
  protected readonly counselors = signal<{ id: string; name: string }[]>([]);
  protected selectedCounselorId = '';

  // Helper flags for tracking empty states
  protected readonly hasFunnelData = signal(false);
  protected readonly hasDropOffData = signal(false);
  protected readonly hasTimeseriesData = signal(false);
  protected readonly hasCountryData = signal(false);
  protected readonly hasChannelData = signal(false);

  // Bound to the period-filter date inputs.
  protected from = '';
  protected to = '';

  constructor() {
    this.load();
    this.loadCounselors();
  }

  private loadCounselors(): void {
    // Make sure catalogs/settings are loaded
    if (!this.catalogService.settings()) {
      this.catalogService.loadAll();
    }
    this.usersService.list().subscribe({
      next: (list) => {
        this.counselors.set(list.map(u => ({ id: u.id, name: u.name })));
      },
      error: () => {}
    });
  }

  protected onCounselorChange(id: string): void {
    this.selectedCounselorId = id;
    this.load();
  }

  protected applyPeriod(): void {
    this.load();
  }

  protected clearPeriod(): void {
    this.from = '';
    this.to = '';
    this.load();
  }

  protected selectPreset(days: number | null): void {
    if (days === null) {
      this.from = '';
      this.to = '';
    } else {
      const end = new Date();
      const start = new Date();
      start.setDate(end.getDate() - days);
      this.from = start.toISOString().split('T')[0];
      this.to = end.toISOString().split('T')[0];
    }
    this.load();
  }

  protected print(): void {
    window.print();
  }

  private load(): void {
    const fail = () => this.offline.set(true);
    const range: DateRange = {
      from: this.from || undefined,
      to: this.to || undefined,
      assignedTo: this.selectedCounselorId || undefined
    };

    this.analytics.summary(range).subscribe({ next: (s) => this.summary.set(s), error: fail });

    this.analytics.funnel(range).subscribe({
      next: (stages) => {
        const total = stages.reduce((acc, curr) => acc + curr.count, 0);
        this.hasFunnelData.set(total > 0);

        const statusColor: Record<string, string> = {
          LEAD: '#6366F1',
          HOT_LEAD: '#F59E0B',
          APPLICATION: '#0EA5E9',
          STUDENT: '#10B981',
          STALLED: '#EF4444'
        };
        this.funnelChart.set({
          ...baseChartOption(),
          tooltip: { trigger: 'item', formatter: '{b}: {c}' },
          series: [
            {
              type: 'funnel',
              sort: 'none',
              gap: 6,
              left: '10%',
              right: '25%',
              top: '10%',
              bottom: '10%',
              width: '65%',
              minSize: '15%',
              label: {
                show: true,
                position: 'right',
                color: '#F8FAFC',
                fontSize: 12,
                fontWeight: 'bold',
                formatter: '{b}: {c}'
              },
              labelLine: {
                lineStyle: {
                  color: 'rgba(255, 255, 255, 0.25)',
                  width: 1.5
                }
              },
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

    this.analytics.dropOff(range).subscribe({
      next: (rows) => {
        const byReason = new Map<string, number>();
        let totalCount = 0;
        for (const row of rows) {
          byReason.set(row.reasonName, (byReason.get(row.reasonName) ?? 0) + row.count);
          totalCount += row.count;
        }
        this.hasDropOffData.set(totalCount > 0);

        const sorted = [...byReason.entries()].sort((a, b) => a[1] - b[1]);
        this.dropOffChart.set({
          ...baseChartOption(),
          grid: { left: 20, right: 45, top: 20, bottom: 20, containLabel: true },
          xAxis: {
            type: 'value',
            minInterval: 1,
            splitLine: { lineStyle: { color: 'rgba(255, 255, 255, 0.05)' } }
          },
          yAxis: { type: 'category', data: sorted.map(([name]) => name), axisLine: { lineStyle: { color: 'rgba(255, 255, 255, 0.12)' } } },
          series: [
            {
              type: 'bar',
              data: sorted.map(([, count]) => count),
              itemStyle: {
                color: '#EF4444',
                borderRadius: [0, 6, 6, 0]
              },
              label: {
                show: true,
                position: 'right',
                color: '#F8FAFC',
                fontWeight: 'bold'
              }
            }
          ]
        });
      },
      error: fail
    });

    this.analytics.timeseries('week', range).subscribe({
      next: (points) => {
        const total = points.reduce((acc, curr) => acc + curr.created + curr.converted, 0);
        this.hasTimeseriesData.set(total > 0);

        this.timeseriesChart.set({
          ...baseChartOption(),
          tooltip: { trigger: 'axis' },
          legend: { right: 10, top: 0, textStyle: { color: '#94A3B8' } },
          grid: { left: 20, right: 25, top: 50, bottom: 35, containLabel: true },
          xAxis: { type: 'category', data: points.map((p) => p.period), axisLine: { lineStyle: { color: 'rgba(255, 255, 255, 0.12)' } } },
          yAxis: {
            type: 'value',
            minInterval: 1,
            splitLine: { lineStyle: { color: 'rgba(255, 255, 255, 0.05)' } }
          },
          series: [
            {
              name: 'Created',
              type: 'line',
              smooth: true,
              showSymbol: true,
              symbolSize: 8,
              lineStyle: { width: 3 },
              areaStyle: { opacity: 0.1 },
              data: points.map((p) => p.created)
            },
            {
              name: 'Converted',
              type: 'line',
              smooth: true,
              showSymbol: true,
              symbolSize: 8,
              lineStyle: { width: 3 },
              areaStyle: { opacity: 0.1 },
              itemStyle: { color: '#10B981' },
              data: points.map((p) => p.converted)
            }
          ]
        });
      },
      error: fail
    });

    this.analytics.byChannel(range).subscribe({
      next: (rows) => {
        const total = rows.reduce((acc, curr) => acc + curr.total + curr.students, 0);
        this.hasChannelData.set(total > 0);

        this.channelChart.set({
          ...baseChartOption(),
          tooltip: { trigger: 'axis' },
          legend: { right: 10, top: 0, textStyle: { color: '#94A3B8' } },
          grid: { left: 20, right: 25, top: 50, bottom: 55, containLabel: true },
          xAxis: {
            type: 'category',
            data: rows.map((r) => r.name),
            axisLabel: { rotate: 25, interval: 0, color: '#94A3B8' },
            axisLine: { lineStyle: { color: 'rgba(255, 255, 255, 0.12)' } }
          },
          yAxis: {
            type: 'value',
            minInterval: 1,
            splitLine: { lineStyle: { color: 'rgba(255, 255, 255, 0.05)' } }
          },
          series: [
            {
              name: 'Leads',
              type: 'bar',
              barMaxWidth: 30,
              data: rows.map((r) => r.total),
              itemStyle: { borderRadius: [4, 4, 0, 0] }
            },
            {
              name: 'Students',
              type: 'bar',
              barMaxWidth: 30,
              itemStyle: { color: '#10B981', borderRadius: [4, 4, 0, 0] },
              data: rows.map((r) => r.students)
            }
          ]
        });
      },
      error: fail
    });

    this.analytics.byCountry(range).subscribe({
      next: (rows) => {
        const total = rows.reduce((acc, curr) => acc + curr.total + curr.students, 0);
        this.hasCountryData.set(total > 0);

        const top = rows.slice(0, 12).reverse();
        this.countryChart.set({
          ...baseChartOption(),
          grid: { left: 20, right: 45, top: 20, bottom: 20, containLabel: true },
          xAxis: {
            type: 'value',
            minInterval: 1,
            splitLine: { lineStyle: { color: 'rgba(255, 255, 255, 0.05)' } }
          },
          yAxis: {
            type: 'category',
            data: top.map((r) => r.countryCode),
            axisLine: { lineStyle: { color: 'rgba(255, 255, 255, 0.12)' } }
          },
          series: [
            {
              type: 'bar',
              data: top.map((r) => r.total),
              itemStyle: {
                color: '#6366F1',
                borderRadius: [0, 6, 6, 0]
              },
              label: {
                show: true,
                position: 'right',
                color: '#F8FAFC',
                fontWeight: 'bold'
              }
            }
          ]
        });
      },
      error: fail
    });

    if (this.auth.isAdmin()) {
      this.analytics.leaderboard(range).subscribe({
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
