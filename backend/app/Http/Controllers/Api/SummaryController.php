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

        return response()->json(ExpenseSummary::for($request->user(), $period));
    }
}
