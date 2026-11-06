<!DOCTYPE html>
<html lang="id">
<head>
    <meta charset="utf-8">
    <title>Mutasi Pengeluaran Costa</title>
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
    <h1>Mutasi Pengeluaran Costa</h1>
    <div class="meta">
        {{ $user->name }} &middot; {{ $from->format('d M Y') }} &mdash; {{ $to->format('d M Y') }} &middot;
        {{ $expenses->count() }} transaksi
    </div>
    <table>
        <thead>
            <tr>
                <th>Tanggal</th>
                <th>Kategori</th>
                <th>Catatan</th>
                <th class="amount">Nominal</th>
            </tr>
        </thead>
        <tbody>
            @forelse ($expenses as $expense)
                <tr>
                    <td>{{ $expense->spent_at->format('d M Y H:i') }}</td>
                    <td>{{ $expense->category?->name ?? '-' }}</td>
                    <td>{{ $expense->note ?? '' }}</td>
                    <td class="amount">Rp{{ number_format($expense->amount, 0, ',', '.') }}</td>
                </tr>
            @empty
                <tr>
                    <td colspan="4" class="muted">Belum ada pengeluaran pada periode ini.</td>
                </tr>
            @endforelse
        </tbody>
        <tfoot>
            <tr>
                <td colspan="3">Total</td>
                <td class="amount">Rp{{ number_format($total, 0, ',', '.') }}</td>
            </tr>
        </tfoot>
    </table>
</body>
</html>
