<?php

namespace App\Http\Requests;

use Illuminate\Foundation\Http\FormRequest;

class StoreExpenseRequest extends FormRequest
{
    public function authorize(): bool
    {
        return true;
    }

    public function rules(): array
    {
        return [
            'amount' => ['required', 'integer', 'min:1'],
            'category' => ['required', 'string', 'max:50'],
            'note' => ['nullable', 'string', 'max:255'],
            'spent_at' => ['nullable', 'date'],
            'source' => ['nullable', 'string', 'max:20'],
            'raw_text' => ['nullable', 'string', 'max:255'],
        ];
    }
}
