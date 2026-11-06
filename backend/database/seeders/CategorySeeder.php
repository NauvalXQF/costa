<?php

namespace Database\Seeders;

use App\Models\Category;
use Illuminate\Database\Seeder;

class CategorySeeder extends Seeder
{
    public function run(): void
    {
        $categories = [
            ['name' => 'Makan', 'slug' => 'makan', 'icon' => '🍚', 'color' => '#F97316', 'aliases' => ['makan', 'sarapan', 'makan siang', 'makan malam', 'nasi', 'warteg', 'gofood', 'grabfood', 'jajan'], 'sort_order' => 1],
            ['name' => 'Bensin', 'slug' => 'bensin', 'icon' => '⛽', 'color' => '#EF4444', 'aliases' => ['bensin', 'pertal', 'pertalite', 'pertamax', 'bbm'], 'sort_order' => 2],
            ['name' => 'Gojek/Grab', 'slug' => 'gojek-grab', 'icon' => '🛵', 'color' => '#16A34A', 'aliases' => ['gojek', 'grab', 'ojek', 'ojol', 'goride', 'gocar', 'grabcar', 'grabbike', 'taksi', 'travel'], 'sort_order' => 3],
            ['name' => 'Listrik', 'slug' => 'listrik', 'icon' => '⚡', 'color' => '#FACC15', 'aliases' => ['listrik', 'token listrik', 'pln', 'pulsa listrik'], 'sort_order' => 4],
            ['name' => 'Kopi', 'slug' => 'kopi', 'icon' => '☕', 'color' => '#A16207', 'aliases' => ['kopi', 'ngopi', 'latte', 'americano', 'cafe', 'es kopi'], 'sort_order' => 5],
            ['name' => 'Gym', 'slug' => 'gym', 'icon' => '🏋️', 'color' => '#22C55E', 'aliases' => ['gym', 'fitness', 'ngegym'], 'sort_order' => 6],
            ['name' => 'Parkir', 'slug' => 'parkir', 'icon' => '🅿️', 'color' => '#3B82F6', 'aliases' => ['parkir', 'parkiran'], 'sort_order' => 7],
            ['name' => 'Kos', 'slug' => 'kos', 'icon' => '🏠', 'color' => '#8B5CF6', 'aliases' => ['kos', 'kost', 'sewa kos', 'uang kos'], 'sort_order' => 8],
            ['name' => 'Laundry', 'slug' => 'laundry', 'icon' => '🧺', 'color' => '#06B6D4', 'aliases' => ['laundry', 'cuci baju', 'kebersihan'], 'sort_order' => 9],
            ['name' => 'Kesehatan', 'slug' => 'kesehatan', 'icon' => '🩺', 'color' => '#EC4899', 'aliases' => ['obat', 'kesehatan', 'vitamin', 'klinik'], 'sort_order' => 10],
            ['name' => 'Hiburan', 'slug' => 'hiburan', 'icon' => '🎮', 'color' => '#F43F5E', 'aliases' => ['nonton', 'bioskop', 'game', 'hiburan', 'spotify', 'netflix'], 'sort_order' => 11],
            ['name' => 'Internet', 'slug' => 'internet', 'icon' => '📶', 'color' => '#0EA5E9', 'aliases' => ['pulsa', 'kuota', 'internet', 'wifi', 'data'], 'sort_order' => 12],
            ['name' => 'Lainnya', 'slug' => 'lainnya', 'icon' => '🧾', 'color' => '#64748B', 'aliases' => ['lain', 'lainnya', 'misc'], 'sort_order' => 13],
        ];

        foreach ($categories as $category) {
            Category::query()->updateOrCreate(
                ['slug' => $category['slug']],
                $category
            );
        }
    }
}
