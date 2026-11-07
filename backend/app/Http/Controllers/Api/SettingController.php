<?php

namespace App\Http\Controllers\Api;
use App\Http\Controllers\Controller;

use App\Models\Setting;
use App\Models\User;
use App\Services\BudgetPlan;
use Illuminate\Http\Request;
use Illuminate\Support\Carbon;

class SettingController extends Controller
{
    public function index(Request $request)
    {
        return response()->json(['data' => $this->payload($request->user())]);
    }

    public function update(Request $request)
    {
        $data = $request->validate([
            'daily_budget' => ['nullable', 'integer', 'min:0'],
            'weekly_budget' => ['nullable', 'integer', 'min:0'],
            'monthly_budget' => ['nullable', 'integer', 'min:0'],
            'food_daily_budget' => ['nullable', 'integer', 'min:0'],
            'budget_start_date' => ['nullable', 'date_format:Y-m-d'],
        ]);

        $userId = $request->user()->id;

        foreach (['daily_budget', 'weekly_budget', 'monthly_budget'] as $key) {
            if (array_key_exists($key, $data) && $data[$key] !== null) {
                Setting::putFor($userId, $key, (int) $data[$key]);
            }
        }

        if (array_key_exists('budget_start_date', $data) && $data['budget_start_date'] !== null) {
            $date = Carbon::parse($data['budget_start_date'])->startOfDay();
            $today = Carbon::today();
            if ($date->year !== $today->year || $date->month !== $today->month || $date->greaterThan($today)) {
                $date = $today->copy();
            }
            Setting::putFor($userId, 'budget_start_date', $date->toDateString());
        }

        if (array_key_exists('food_daily_budget', $data) && $data['food_daily_budget'] !== null) {
            $newValue = (int) $data['food_daily_budget'];
            $oldValue = (int) Setting::getFor($userId, 'food_daily_budget', 0);

            Setting::putFor($userId, 'food_daily_budget', $newValue);

            if ($oldValue !== $newValue) {
                Setting::putFor($userId, 'food_budget_start', Carbon::today()->toDateString());
            }
        }

        return response()->json(['data' => $this->payload($request->user())]);
    }

    private function payload(User $user): array
    {
        $plan = BudgetPlan::for($user);

        return [
            'daily_budget' => $plan['daily'],
            'weekly_budget' => $plan['weekly'],
            'monthly_budget' => $plan['monthly'],
            'food_daily_budget' => (int) Setting::getFor($user->id, 'food_daily_budget', 0),
            'budget_start_date' => $plan['period_start'],
            'period_start' => $plan['period_start'],
            'period_end' => $plan['period_end'],
            'period_days' => $plan['period_days'],
            'week_start' => $plan['week_start'],
            'week_end' => $plan['week_end'],
            'week_days' => $plan['week_days'],
        ];
    }
}
