<?php

namespace App\Services;

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
        ];
    }
}
