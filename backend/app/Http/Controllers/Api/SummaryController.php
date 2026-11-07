<?php

namespace App\Http\Controllers\Api;

use App\Http\Controllers\Controller;
use App\Services\ExpenseSummary;
use Illuminate\Http\Request;

class SummaryController extends Controller
{
    public function __invoke(Request $request)
    {
        $period = (string) $request->query('period', 'today');
        $month = $request->query('month');
        if (! is_string($month) || ! preg_match('/^\d{4}-(0[1-9]|1[0-2])$/', $month)) {
            $month = null;
        }

        return response()->json(ExpenseSummary::for($request->user(), $period, $month));
    }
}
