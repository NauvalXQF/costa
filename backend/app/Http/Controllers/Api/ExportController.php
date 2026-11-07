<?php

namespace App\Http\Controllers\Api;

use App\Http\Controllers\Controller;
use App\Models\Expense;
use Barryvdh\DomPDF\Facade\Pdf;
use Illuminate\Http\Request;
use Illuminate\Support\Carbon;

class ExportController extends Controller
{
    private const ENGLISH_CATEGORIES = [
        'makan' => 'Food',
        'bensin' => 'Fuel',
        'gojek-grab' => 'Gojek/Grab',
        'listrik' => 'Electricity',
        'kopi' => 'Coffee',
        'gym' => 'Gym',
        'parkir' => 'Parking',
        'kos' => 'Rent',
        'laundry' => 'Laundry',
        'kesehatan' => 'Health',
        'hiburan' => 'Entertainment',
        'internet' => 'Internet',
        'lainnya' => 'Other',
        'jajan' => 'Snacks',
    ];

    public function csv(Request $request)
    {
        [$from, $to, $expenses, $lang] = $this->collect($request);

        $filename = 'costa-mutasi-'.$from->format('Y-m-d').'-'.$to->format('Y-m-d').'.csv';
        $headers = $lang === 'en'
            ? ['Date', 'Category', 'Note', 'Amount']
            : ['Tanggal', 'Kategori', 'Catatan', 'Nominal'];

        $callback = function () use ($expenses, $headers, $lang) {
            $out = fopen('php://output', 'w');
            fwrite($out, "\xEF\xBB\xBF");
            fputcsv($out, $headers);

            foreach ($expenses as $expense) {
                fputcsv($out, [
                    $expense->spent_at->format('Y-m-d H:i'),
                    $this->categoryName($expense, $lang),
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
        [$from, $to, $expenses, $lang, $total] = $this->collect($request);
        $categoryNames = $expenses->mapWithKeys(
            fn (Expense $expense) => [$expense->id => $this->categoryName($expense, $lang)],
        );

        $pdf = Pdf::loadView('exports.mutations', [
            'user' => $request->user(),
            'from' => $from,
            'to' => $to,
            'expenses' => $expenses,
            'categoryNames' => $categoryNames,
            'total' => $total,
            'lang' => $lang,
        ])->setPaper('a4');

        return $pdf->download('costa-mutasi-'.$from->format('Y-m-d').'-'.$to->format('Y-m-d').'.pdf');
    }

    private function collect(Request $request): array
    {
        $lang = $request->query('lang') === 'en' ? 'en' : 'id';

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

        return [$from, $to, $expenses, $lang, (int) $expenses->sum('amount')];
    }

    private function categoryName(Expense $expense, string $lang): string
    {
        $category = $expense->category;

        if (! $category) {
            return '-';
        }

        if ($lang !== 'en' || $category->user_id !== null) {
            return $category->name;
        }

        return self::ENGLISH_CATEGORIES[$category->slug] ?? $category->name;
    }
}
