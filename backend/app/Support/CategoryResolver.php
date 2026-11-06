<?php

namespace App\Support;

use App\Models\Category;

class CategoryResolver
{
    public static function resolve(string $input, ?int $userId = null): ?Category
    {
        $needle = mb_strtolower(trim($input));

        if ($needle === '') {
            return null;
        }

        $base = Category::query()->active();

        if ($userId !== null) {
            $base->visibleTo($userId);
        }

        $direct = (clone $base)
            ->where(function ($query) use ($needle) {
                $query->whereRaw('lower(slug) = ?', [$needle])
                    ->orWhereRaw('lower(name) = ?', [$needle]);
            })
            ->first();

        if ($direct) {
            return $direct;
        }

        return (clone $base)
            ->whereJsonContains('aliases', $needle)
            ->first();
    }
}
