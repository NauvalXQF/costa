<?php

namespace App\Http\Controllers\Api;

use App\Http\Controllers\Controller;
use App\Models\Expense;
use Barryvdh\DomPDF\Facade\Pdf;
use Illuminate\Http\Request;
use Illuminate\Support\Carbon;

class ExportController extends Controller
{
    public function csv(Request $request)
    {
        [$from, $to, $expenses] = $this->collect($request);

        $filename = 'costa-mutasi-'.$from->format('Y-m-d').'-'.$to->format('Y-m-d').'.csv';

        $callback = function () use ($expenses) {
            $out = fopen('php://output', 'w');
            fwrite($out, "\xEF\xBB\xBF");
            fputcsv($out, ['Tanggal', 'Kategori', 'Catatan', 'Nominal']);

            foreach ($expenses as $expense) {
                fputcsv($out, [
                    $expense->spent_at->format('Y-m-d H:i'),
                    $expense->category?->name ?? '-',
                    $expense->note ?? '',
                    $expense->amount,
                ]);
            }

            fclose($out);
        };

        return response()->streamDownload($callback, $filename, [
            'Content-Type' => 'text/csv; charset=UTF-8',
        ]);
    }

    public function pdf(Request $request)
    {
        [$from, $to, $expenses, $total] = $this->collect($request);

        $pdf = Pdf::loadView('exports.mutations', [
            'user' => $request->user(),
            'from' => $from,
            'to' => $to,
            'expenses' => $expenses,
            'total' => $total,
        ])->setPaper('a4');

        return $pdf->download('costa-mutasi-'.$from->format('Y-m-d').'-'.$to->format('Y-m-d').'.pdf');
    }

    private function collect(Request $request): array
    {
        $from = $request->filled('from')
            ? Carbon::parse((string) $request->query('from'))->startOfDay()
            : Carbon::now()->startOfMonth();

        $to = $request->filled('to')
            ? Carbon::parse((string) $request->query('to'))->endOfDay()
            : Carbon::now()->endOfDay();

        $expenses = Expense::query()
            ->where('user_id', $request->user()->id)
            ->with('category')
            ->whereBetween('spent_at', [$from, $to])
            ->orderBy('spent_at')
            ->get();

        return [$from, $to, $expenses, (int) $expenses->sum('amount')];
    }
}
