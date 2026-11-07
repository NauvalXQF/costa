<?php

namespace App\Services;

use App\Models\Expense;
use App\Models\Setting;
use App\Models\User;
use Illuminate\Support\Carbon;

class BudgetPlan
{
    public static function for(User $user, ?Carbon $now = null): array
    {
        $now = ($now ?? Carbon::now())->copy();
        $monthStart = $now->copy()->startOfMonth()->startOfDay();
        $monthEnd = $now->copy()->endOfMonth()->startOfDay();

        $monthly = (int) Setting::getFor($user->id, 'monthly_budget', 0);
        $legacyDaily = (int) Setting::getFor($user->id, 'daily_budget', 0);
        $legacyWeekly = (int) Setting::getFor($user->id, 'weekly_budget', 0);

        $periodStart = self::periodStart($user->id, $now, $monthStart, $monthEnd);
        $periodDays = max(1, (int) $periodStart->diffInDays($monthEnd) + 1);
        $daily = $monthly > 0 ? intdiv($monthly, $periodDays) : $legacyDaily;

        $weekStart = $now->copy()->startOfWeek()->startOfDay();
        $weekEnd = $now->copy()->endOfWeek()->startOfDay();
        $effectiveWeekStart = $weekStart->greaterThan($periodStart) ? $weekStart->copy() : $periodStart->copy();
        $effectiveWeekEnd = $weekEnd->lessThan($monthEnd) ? $weekEnd->copy() : $monthEnd->copy();
        $weekDays = $effectiveWeekStart->lessThanOrEqualTo($effectiveWeekEnd)
            ? (int) $effectiveWeekStart->diffInDays($effectiveWeekEnd) + 1
            : 0;
        $weekly = $monthly > 0 ? $daily * $weekDays : $legacyWeekly;

        return [
            'monthly' => $monthly,
            'daily' => $daily,
            'weekly' => $weekly,
            'period_start' => $periodStart->toDateString(),
            'period_end' => $monthEnd->toDateString(),
            'period_days' => $periodDays,
            'week_start' => $weekDays > 0 ? $effectiveWeekStart->toDateString() : null,
            'week_end' => $weekDays > 0 ? $effectiveWeekEnd->toDateString() : null,
            'week_days' => $weekDays,
        ];
    }

    public static function periodStart(int $userId, Carbon $now, ?Carbon $monthStart = null, ?Carbon $monthEnd = null): Carbon
    {
        $monthStart = ($monthStart ?? $now->copy()->startOfMonth())->copy()->startOfDay();
        $monthEnd = ($monthEnd ?? $now->copy()->endOfMonth())->copy()->startOfDay();

        $stored = Setting::getFor($userId, 'budget_start_date');
        if (is_string($stored) && trim($stored) !== '') {
            try {
                $date = Carbon::parse($stored)->startOfDay();
                $inMonth = $date->year === $now->year && $date->month === $now->month;
                if ($inMonth && $date->greaterThanOrEqualTo($monthStart) && $date->lessThanOrEqualTo($monthEnd)) {
                    return $date;
                }
            } catch (\Throwable) {
            }
        }

        $first = Expense::query()
            ->where('user_id', $userId)
            ->whereBetween('spent_at', [$monthStart, $monthEnd->copy()->endOfDay()])
            ->selectRaw('MIN(date(spent_at)) AS first_date')
            ->first();

        $firstDate = $first?->first_date;
        if (is_string($firstDate) && $firstDate !== '') {
            $date = Carbon::parse($firstDate)->startOfDay();
            if ($date->greaterThanOrEqualTo($monthStart) && $date->lessThanOrEqualTo($monthEnd)) {
                return $date;
            }
        }

        return $monthStart;
    }
}
