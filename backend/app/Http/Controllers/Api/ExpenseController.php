<?php

namespace App\Http\Controllers\Api;

use App\Http\Controllers\Controller;
use App\Http\Requests\StoreExpenseRequest;
use App\Http\Requests\UpdateExpenseRequest;
use App\Http\Resources\ExpenseResource;
use App\Models\Expense;
use App\Support\CategoryResolver;
use Illuminate\Http\Request;
use Illuminate\Support\Carbon;
use Illuminate\Validation\ValidationException;

class ExpenseController extends Controller
{
    public function index(Request $request)
    {
        $query = Expense::query()
            ->where('user_id', $request->user()->id)
            ->with('category')
            ->orderByDesc('spent_at')
            ->orderByDesc('id');

        if ($before = $request->query('before')) {
            $beforeAt = Carbon::parse((string) $before);
            $beforeId = (int) $request->query('before_id', 0);

            $query->where(function ($query) use ($beforeAt, $beforeId) {
                $query->where('spent_at', '<', $beforeAt)
                    ->orWhere(function ($query) use ($beforeAt, $beforeId) {
                        $query->where('spent_at', $beforeAt)
                            ->where('id', '<', $beforeId);
                    });
            });
        }

        if ($from = $request->query('from')) {
            $query->where('spent_at', '>=', Carbon::parse($from)->startOfDay());
        }

        if ($to = $request->query('to')) {
            $query->where('spent_at', '<=', Carbon::parse($to)->endOfDay());
        }

        if ($request->filled('category')) {
            $category = CategoryResolver::resolve((string) $request->query('category'), $request->user()->id);

            if ($category) {
                $query->where('category_id', $category->id);
            }
        }

        $limit = min(max((int) $request->query('limit', 100), 1), 500);

        return ExpenseResource::collection($query->limit($limit)->get());
    }

    public function store(StoreExpenseRequest $request)
    {
        $data = $request->validated();
        $category = CategoryResolver::resolve((string) $data['category'], $request->user()->id);

        if (! $category) {
            throw ValidationException::withMessages([
                'category' => 'Kategori tidak dikenal.',
            ]);
        }

        $expense = Expense::query()->create([
            'user_id' => $request->user()->id,
            'category_id' => $category->id,
            'amount' => (int) $data['amount'],
            'note' => $data['note'] ?? null,
            'spent_at' => isset($data['spent_at']) ? Carbon::parse($data['spent_at']) : now(),
            'source' => $data['source'] ?? 'api',
            'raw_text' => $data['raw_text'] ?? null,
        ]);

        return (new ExpenseResource($expense->load('category')))
            ->response()
            ->setStatusCode(201);
    }

    public function show(Request $request, Expense $expense)
    {
        $this->authorizeExpense($request, $expense);

        return new ExpenseResource($expense->load('category'));
    }

    public function update(UpdateExpenseRequest $request, Expense $expense)
    {
        $this->authorizeExpense($request, $expense);

        $data = $request->validated();

        if (array_key_exists('category', $data)) {
            $category = CategoryResolver::resolve((string) $data['category'], $request->user()->id);

            if (! $category) {
                throw ValidationException::withMessages([
                    'category' => 'Kategori tidak dikenal.',
                ]);
            }

            $data['category_id'] = $category->id;
            unset($data['category']);
        }

        if (isset($data['amount'])) {
            $data['amount'] = (int) $data['amount'];
        }

        if (isset($data['spent_at'])) {
            $data['spent_at'] = Carbon::parse($data['spent_at']);
        }

        $expense->update($data);

        return new ExpenseResource($expense->load('category'));
    }

    public function destroy(Request $request, Expense $expense)
    {
        $this->authorizeExpense($request, $expense);

        $expense->delete();

        return response()->json(['message' => 'Pengeluaran dihapus.']);
    }

    private function authorizeExpense(Request $request, Expense $expense): void
    {
        abort_unless($expense->user_id === $request->user()->id, 404);
    }
}
