<?php

use Illuminate\Database\Migrations\Migration;
use Illuminate\Database\Schema\Blueprint;
use Illuminate\Support\Facades\Schema;

return new class extends Migration
{
    public function up(): void
    {
        Schema::dropIfExists('expenses');
        Schema::dropIfExists('incomes');

        Schema::create('expenses', function (Blueprint $table) {
            $table->id();
            $table->foreignId('user_id')->constrained()->cascadeOnDelete();
            $table->foreignId('category_id')->constrained()->restrictOnDelete();
            $table->unsignedInteger('amount');
            $table->string('note', 255)->nullable();
            $table->dateTime('spent_at')->index();
            $table->string('source', 20)->default('manual');
            $table->string('raw_text', 255)->nullable();
            $table->timestamps();
            $table->index(['user_id', 'spent_at']);
        });

        Schema::create('incomes', function (Blueprint $table) {
            $table->id();
            $table->foreignId('user_id')->constrained()->cascadeOnDelete();
            $table->unsignedInteger('amount');
            $table->string('source', 50)->default('lainnya');
            $table->string('note', 255)->nullable();
            $table->dateTime('received_at')->index();
            $table->timestamps();
            $table->index(['user_id', 'received_at']);
        });
    }

    public function down(): void
    {
        Schema::dropIfExists('incomes');
        Schema::dropIfExists('expenses');

        Schema::create('expenses', function (Blueprint $table) {
            $table->id();
            $table->foreignId('category_id')->constrained()->restrictOnDelete();
            $table->unsignedInteger('amount');
            $table->string('note', 255)->nullable();
            $table->dateTime('spent_at')->index();
            $table->string('source', 20)->default('manual');
            $table->string('raw_text', 255)->nullable();
            $table->timestamps();
        });

        Schema::create('incomes', function (Blueprint $table) {
            $table->id();
            $table->unsignedInteger('amount');
            $table->string('source', 50)->default('lainnya');
            $table->string('note', 255)->nullable();
            $table->dateTime('received_at')->index();
            $table->timestamps();
        });
    }
};
