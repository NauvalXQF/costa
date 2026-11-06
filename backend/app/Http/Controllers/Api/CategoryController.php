<?php

namespace App\Http\Controllers\Api;

use App\Http\Controllers\Controller;
use App\Models\Category;
use App\Models\Setting;
use Illuminate\Http\Request;
use Illuminate\Support\Str;
use Illuminate\Validation\ValidationException;

class CategoryController extends Controller
{
    public function index(Request $request)
    {
        $userId = $request->user()->id;

        $hidden = Setting::getFor($userId, 'hidden_categories', []);
        $hidden = is_array($hidden) ? array_values($hidden) : [];

        $categories = Category::query()
            ->active()
            ->visibleTo($userId)
            ->whereNotIn('slug', $hidden)
            ->get();

        $order = Setting::getFor($userId, 'category_order', []);
        $position = is_array($order) ? array_flip($order) : [];

        $sorted = $categories
            ->sortBy(function (Category $category) use ($position) {
                return $position[$category->slug] ?? (1000 + $category->sort_order * 10 + $category->id);
            })
            ->values();

        return response()->json([
            'data' => $sorted->map(fn (Category $category) => $this->payload($category)),
            'hidden' => $hidden,
        ]);
    }

    public function store(Request $request)
    {
        $data = $request->validate([
            'name' => ['required', 'string', 'max:50'],
            'icon' => ['nullable', 'string', 'max:10'],
            'color' => ['nullable', 'string', 'max:7'],
            'aliases' => ['nullable', 'array'],
            'aliases.*' => ['string', 'max:30'],
        ]);

        $base = Str::slug($data['name']);

        if ($base === '') {
            throw ValidationException::withMessages(['name' => 'Nama kategori tidak valid.']);
        }

        $visibleExists = Category::query()
            ->where('slug', $base)
            ->visibleTo($request->user()->id)
            ->exists();

        if ($visibleExists) {
            throw ValidationException::withMessages(['name' => 'Kategori sudah ada.']);
        }

        $slug = $base;

        if (Category::query()->where('slug', $slug)->exists()) {
            $slug = $base.'-'.$request->user()->id;
        }

        if (Category::query()->where('slug', $slug)->exists()) {
            $slug = $base.'-'.$request->user()->id.'-'.Str::lower(Str::random(3));
        }

        $category = Category::query()->create([
            'user_id' => $request->user()->id,
            'name' => $data['name'],
            'slug' => $slug,
            'icon' => $data['icon'] ?? '🏷️',
            'color' => $data['color'] ?? '#94A3B8',
            'aliases' => $data['aliases'] ?? [mb_strtolower($data['name'])],
            'sort_order' => 100,
            'is_active' => true,
        ]);

        return response()->json(['data' => $this->payload($category)], 201);
    }

    public function update(Request $request, Category $category)
    {
        abort_unless($category->user_id === $request->user()->id, 404);

        $data = $request->validate([
            'name' => ['sometimes', 'string', 'max:50'],
            'icon' => ['nullable', 'string', 'max:10'],
            'color' => ['nullable', 'string', 'max:7'],
            'aliases' => ['nullable', 'array'],
            'aliases.*' => ['string', 'max:30'],
        ]);

        $category->update($data);

        return response()->json(['data' => $this->payload($category->refresh())]);
    }

    public function reorder(Request $request)
    {
        $data = $request->validate([
            'order' => ['required', 'array', 'min:1'],
            'order.*' => ['string', 'max:50'],
        ]);

        $visible = Category::query()
            ->active()
            ->visibleTo($request->user()->id)
            ->pluck('slug')
            ->all();

        $clean = array_values(array_intersect($data['order'], $visible));

        Setting::putFor($request->user()->id, 'category_order', $clean);

        return response()->json(['data' => $clean]);
    }

    public function restore(Request $request)
    {
        Setting::putFor($request->user()->id, 'hidden_categories', []);

        return response()->json(['message' => 'Kategori tersembunyi dipulihkan.']);
    }

    public function destroy(Request $request, Category $category)
    {
        $user = $request->user();

        if ($category->user_id === null) {
            $hidden = Setting::getFor($user->id, 'hidden_categories', []);
            $hidden = is_array($hidden) ? array_values($hidden) : [];

            if (! in_array($category->slug, $hidden, true)) {
                $hidden[] = $category->slug;
            }

            Setting::putFor($user->id, 'hidden_categories', $hidden);

            return response()->json(['message' => 'Kategori bawaan disembunyikan.']);
        }

        abort_unless($category->user_id === $user->id, 404);

        if ($category->expenses()->where('user_id', $user->id)->exists()) {
            throw ValidationException::withMessages([
                'category' => 'Kategori masih dipakai transaksi.',
            ]);
        }

        $category->delete();

        return response()->json(['message' => 'Kategori dihapus.']);
    }

    private function payload(Category $category): array
    {
        return [
            'id' => $category->id,
            'user_id' => $category->user_id,
            'name' => $category->name,
            'slug' => $category->slug,
            'icon' => $category->icon,
            'color' => $category->color,
            'aliases' => $category->aliases,
        ];
    }
}
