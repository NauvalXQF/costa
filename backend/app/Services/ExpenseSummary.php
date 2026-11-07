<?php

namespace App\Services;

use App\Models\Category;
use App\Models\Expense;
use App\Models\Setting;
use App\Models\User;
use Illuminate\Support\Carbon;

class ExpenseSummary
{
    public static function for(User $user, string $period = 'today'): array
    {
        $now = Carbon::now();
        $label = in_array($period, ['week', 'month'], true) ? $period : 'today';

        [$start, $end] = match ($label) {
            'week' => [$now->copy()->startOfWeek(), $now->copy()->endOfWeek()],
            'month' => [$now->copy()->startOfMonth(), $now->copy()->endOfMonth()],
            default => [$now->copy()->startOfDay(), $now->copy()->endOfDay()],
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

        $daily = (int) Setting::getFor($user->id, 'daily_budget', 0);
        $weekly = (int) Setting::getFor($user->id, 'weekly_budget', 0);
        $monthly = (int) Setting::getFor($user->id, 'monthly_budget', 0);

        $limit = match ($label) {
            'week' => $weekly,
            'month' => $monthly,
            default => $daily,
        };

        $remaining = $limit > 0 ? max($limit - $total, 0) : null;
        $percent = $limit > 0 ? round($total / $limit * 100, 1) : null;

        $monthSpent = (int) Expense::query()
            ->where('user_id', $user->id)
            ->whereBetween('spent_at', [$now->copy()->startOfMonth(), $now->copy()->endOfMonth()])
            ->sum('amount');

        $daysLeft = $now->daysInMonth - $now->day + 1;
        $monthRemaining = $monthly > 0 ? max($monthly - $monthSpent, 0) : null;
        $safeToSpendPerDay = ($monthly > 0 && $daysLeft > 0)
            ? (int) floor($monthRemaining / $daysLeft)
            : null;

        return [
            'period' => [
                'label' => $label,
                'start' => $start->toIso8601String(),
                'end' => $end->toIso8601String(),
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
                'budget' => $monthly ?: null,
                'spent' => $monthSpent,
                'remaining' => $monthRemaining,
                'safe_to_spend_per_day' => $safeToSpendPerDay,
            ],
            'food_budget' => self::foodBudget($user, $now),
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

        $allowanceToday = $base + ($base * ($days - 1)) - $spentBeforeToday;
        $balance = ($base * $days) - $spentSinceStart;

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
