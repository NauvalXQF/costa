<!DOCTYPE html>
<html lang="{{ ($lang ?? 'id') === 'en' ? 'en' : 'id' }}">
<head>
    <meta charset="utf-8">
    <title>{{ ($lang ?? 'id') === 'en' ? 'Costa Expense Mutations' : 'Mutasi Pengeluaran Costa' }}</title>
    <style>
        body { font-family: DejaVu Sans, sans-serif; font-size: 11px; color: #111; }
        h1 { font-size: 18px; margin: 0 0 4px; }
        .meta { color: #555; margin-bottom: 16px; }
        table { width: 100%; border-collapse: collapse; }
        th { background: #f2f2f2; text-align: left; padding: 6px; border-bottom: 1px solid #ccc; }
        td { padding: 5px 6px; border-bottom: 1px solid #eee; }
        .amount { text-align: right; white-space: nowrap; }
        tfoot td { font-weight: bold; border-top: 1px solid #333; }
        .muted { color: #666; }
    </style>
</head>
<body>
    @php
        $isEn = ($lang ?? 'id') === 'en';
        $locale = $isEn ? 'en' : 'id';
        $countLabel = $isEn
            ? $expenses->count() . ' ' . ($expenses->count() === 1 ? 'transaction' : 'transactions')
            : $expenses->count() . ' transaksi';
    @endphp
    <h1>{{ $isEn ? 'Costa Expense Mutations' : 'Mutasi Pengeluaran Costa' }}</h1>
    <div class="meta">
        {{ $user->name }} &middot; {{ $from->locale($locale)->translatedFormat('d M Y') }} &mdash; {{ $to->locale($locale)->translatedFormat('d M Y') }} &middot;
        {{ $countLabel }}
    </div>
    <table>
        <thead>
            <tr>
                <th>{{ $isEn ? 'Date' : 'Tanggal' }}</th>
                <th>{{ $isEn ? 'Category' : 'Kategori' }}</th>
                <th>{{ $isEn ? 'Note' : 'Catatan' }}</th>
                <th class="amount">{{ $isEn ? 'Amount' : 'Nominal' }}</th>
            </tr>
        </thead>
        <tbody>
            @forelse ($expenses as $expense)
                <tr>
                    <td>{{ $expense->spent_at->locale($locale)->translatedFormat('d M Y H:i') }}</td>
                    <td>{{ $categoryNames[$expense->id] ?? ($expense->category?->name ?? '-') }}</td>
                    <td>{{ $expense->note ?? '' }}</td>
                    <td class="amount">Rp{{ $isEn ? number_format($expense->amount, 0, '.', ',') : number_format($expense->amount, 0, ',', '.') }}</td>
                </tr>
            @empty
                <tr>
                    <td colspan="4" class="muted">{{ $isEn ? 'No expenses in this period.' : 'Belum ada pengeluaran pada periode ini.' }}</td>
                </tr>
            @endforelse
        </tbody>
        <tfoot>
            <tr>
                <td colspan="3">{{ $isEn ? 'Total' : 'Total' }}</td>
                <td class="amount">Rp{{ $isEn ? number_format($total, 0, '.', ',') : number_format($total, 0, ',', '.') }}</td>
            </tr>
        </tfoot>
    </table>
</body>
</html>
