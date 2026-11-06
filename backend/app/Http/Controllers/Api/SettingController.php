<?php

namespace App\Http\Controllers\Api;

use App\Http\Controllers\Controller;
use App\Models\Setting;
use Illuminate\Http\Request;

class SettingController extends Controller
{
    public function index(Request $request)
    {
        return response()->json(['data' => $this->payload($request->user()->id)]);
    }

    public function update(Request $request)
    {
        $data = $request->validate([
            'daily_budget' => ['nullable', 'integer', 'min:0'],
            'weekly_budget' => ['nullable', 'integer', 'min:0'],
            'monthly_budget' => ['nullable', 'integer', 'min:0'],
        ]);

        $userId = $request->user()->id;

        foreach (['daily_budget', 'weekly_budget', 'monthly_budget'] as $key) {
            if (array_key_exists($key, $data) && $data[$key] !== null) {
                Setting::putFor($userId, $key, (int) $data[$key]);
            }
        }

        return response()->json(['data' => $this->payload($userId)]);
    }

    private function payload(int $userId): array
    {
        return [
            'daily_budget' => (int) Setting::getFor($userId, 'daily_budget', 0),
            'weekly_budget' => (int) Setting::getFor($userId, 'weekly_budget', 0),
            'monthly_budget' => (int) Setting::getFor($userId, 'monthly_budget', 0),
        ];
    }
}
