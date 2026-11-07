<?php

namespace App\Services;

use App\Models\Category;
use App\Models\Expense;
use App\Models\Setting;
use App\Models\User;
use Illuminate\Support\Carbon;

class ExpenseSummary
{
    public static function for(User $user, string $period = 'today', ?string $month = null): array
    {
        $now = Carbon::now();
        $label = in_array($period, ['week', 'month'], true) ? $period : 'today';

        if ($label === 'month' && $month !== null && preg_match('/^\d{4}-(0[1-9]|1[0-2])$/', $month)) {
            $target = Carbon::createFromFormat('Y-m', $month)->startOfMonth();
            if ($target->year !== $now->year || $target->month !== $now->month) {
                return self::historicalMonth($user, $target);
            }
        }
        $plan = BudgetPlan::for($user, $now);

        $periodStart = Carbon::parse($plan['period_start'])->startOfDay();
        $periodEnd = Carbon::parse($plan['period_end'])->endOfDay();
        $todayStart = $now->copy()->startOfDay();
        $todayEnd = $now->copy()->endOfDay();

        [$start, $end] = match ($label) {
            'week' => [
                $plan['week_start'] ? Carbon::parse($plan['week_start'])->startOfDay() : $now->copy()->startOfWeek(),
                $plan['week_end'] ? Carbon::parse($plan['week_end'])->endOfDay() : $now->copy()->endOfWeek(),
            ],
            'month' => [$periodStart->copy(), $periodEnd->copy()],
            default => [$todayStart->copy(), $todayEnd->copy()],
        };

        $expenses = Expense::query()
            ->where('user_id', $user->id)
            ->with('category')
            ->whereBetween('spent_at', [$start, $end])
            ->get();

        $total = (int) $expenses->sum('amount');
        $count = $expenses->count();

        $byCategory = $expenses
            ->groupBy('category_id')
            ->map(function ($group) {
                $category = $group->first()->category;

                return [
                    'slug' => $category?->slug,
                    'name' => $category?->name,
                    'icon' => $category?->icon,
                    'color' => $category?->color,
                    'total' => (int) $group->sum('amount'),
                    'count' => $group->count(),
                ];
            })
            ->sortByDesc('total')
            ->values();

        $limit = match ($label) {
            'week' => $plan['weekly'],
            'month' => $plan['monthly'],
            default => $plan['daily'],
        };

        $remaining = $limit > 0 ? max($limit - $total, 0) : null;
        $percent = $limit > 0 ? round($total / $limit * 100, 1) : null;

        $monthSpent = (int) Expense::query()
            ->where('user_id', $user->id)
            ->whereBetween('spent_at', [$periodStart, $periodEnd])
            ->sum('amount');

        $daysLeft = 0;
        if ($now->copy()->startOfDay()->lessThanOrEqualTo($periodEnd)) {
            $daysLeft = (int) $now->copy()->startOfDay()->diffInDays($periodEnd->copy()->startOfDay()) + 1;
        }

        $monthRemaining = $plan['monthly'] > 0 ? max($plan['monthly'] - $monthSpent, 0) : null;
        $safeToSpendPerDay = ($plan['monthly'] > 0 && $daysLeft > 0)
            ? (int) floor($monthRemaining / $daysLeft)
            : null;

        return [
            'period' => [
                'label' => $label,
                'start' => $start->toIso8601String(),
                'end' => $end->toIso8601String(),
                'budget_start' => $plan['period_start'],
                'budget_end' => $plan['period_end'],
                'budget_days' => $plan['period_days'],
            ],
            'total' => $total,
            'count' => $count,
            'by_category' => $byCategory,
            'budget' => [
                'limit' => $limit ?: null,
                'spent' => $total,
                'remaining' => $remaining,
                'percent' => $percent,
                'exceeded' => $limit > 0 && $total > $limit,
            ],
            'month' => [
                'budget' => $plan['monthly'] ?: null,
                'spent' => $monthSpent,
                'remaining' => $monthRemaining,
                'safe_to_spend_per_day' => $safeToSpendPerDay,
                'period_start' => $plan['period_start'],
                'period_end' => $plan['period_end'],
                'period_days' => $plan['period_days'],
                'daily_budget' => $plan['daily'],
                'weekly_budget' => $plan['weekly'],
            ],
            'food_budget' => self::foodBudget($user, $now),
        ];
    }

    private static function historicalMonth(User $user, Carbon $target): array
    {
        $start = $target->copy()->startOfMonth();
        $end = $target->copy()->endOfMonth();

        $expenses = Expense::query()
            ->where('user_id', $user->id)
            ->with('category')
            ->whereBetween('spent_at', [$start, $end])
            ->get();

        $total = (int) $expenses->sum('amount');
        $count = $expenses->count();

        $byCategory = $expenses
            ->groupBy('category_id')
            ->map(function ($group) {
                $category = $group->first()->category;

                return [
                    'slug' => $category?->slug,
                    'name' => $category?->name,
                    'icon' => $category?->icon,
                    'color' => $category?->color,
                    'total' => (int) $group->sum('amount'),
                    'count' => $group->count(),
                ];
            })
            ->sortByDesc('total')
            ->values();

        return [
            'period' => [
                'label' => 'month',
                'start' => $start->toIso8601String(),
                'end' => $end->toIso8601String(),
                'budget_start' => $start->toDateString(),
                'budget_end' => $end->toDateString(),
                'budget_days' => $start->daysInMonth,
            ],
            'total' => $total,
            'count' => $count,
            'by_category' => $byCategory,
            'budget' => [
                'limit' => null,
                'spent' => $total,
                'remaining' => null,
                'percent' => null,
                'exceeded' => false,
            ],
            'month' => [
                'budget' => null,
                'spent' => $total,
                'remaining' => null,
                'safe_to_spend_per_day' => null,
                'period_start' => $start->toDateString(),
                'period_end' => $end->toDateString(),
                'period_days' => $start->daysInMonth,
                'daily_budget' => 0,
                'weekly_budget' => 0,
            ],
            'food_budget' => null,
        ];
    }

    private static function foodBudget(User $user, Carbon $now): ?array
    {
        $base = (int) Setting::getFor($user->id, 'food_daily_budget', 0);

        if ($base <= 0) {
            return null;
        }

        $category = Category::query()
            ->whereNull('user_id')
            ->where('slug', 'makan')
            ->first();

        if (! $category) {
            return null;
        }

        $monthStart = $now->copy()->startOfMonth()->startOfDay();
        $todayStart = $now->copy()->startOfDay();

        $startDate = Carbon::parse(
            (string) Setting::getFor($user->id, 'food_budget_start', $todayStart->toDateString()),
        )->startOfDay();

        if ($startDate->lt($monthStart)) {
            $startDate = $monthStart->copy();
        }

        if ($startDate->gt($todayStart)) {
            $startDate = $todayStart->copy();
        }

        $days = (int) $startDate->diffInDays($todayStart) + 1;

        $carry = 0;
        if ((string) Setting::getFor($user->id, 'food_carry_month', '') === $now->format('Y-m')) {
            $carry = (int) Setting::getFor($user->id, 'food_balance_carry', 0);
        }

        $spentSinceStart = (int) Expense::query()
            ->where('user_id', $user->id)
            ->where('category_id', $category->id)
            ->whereBetween('spent_at', [$startDate, $now->copy()->endOfDay()])
            ->sum('amount');

        $spentToday = (int) Expense::query()
            ->where('user_id', $user->id)
            ->where('category_id', $category->id)
            ->whereBetween('spent_at', [$todayStart, $now->copy()->endOfDay()])
            ->sum('amount');

        $spentBeforeToday = $spentSinceStart - $spentToday;

        $allowanceToday = $carry + ($base * $days) - $spentBeforeToday;
        $balance = $carry + ($base * $days) - $spentSinceStart;

        return [
            'base' => $base,
            'start' => $startDate->toDateString(),
            'days' => $days,
            'spent_today' => $spentToday,
            'spent_since_start' => $spentSinceStart,
            'allowance_today' => max(0, (int) $allowanceToday),
            'balance' => (int) $balance,
            'tomorrow_allowance' => max(0, (int) ($base + $balance)),
            'debt' => $balance < 0 ? (int) abs($balance) : 0,
            'bonus' => $balance > 0 ? (int) $balance : 0,
        ];
    }
}