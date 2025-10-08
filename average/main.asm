section .data
    array_x     dq  5, 3, 2, 6, 1, 7, 5
    array_y     dq  0, 10, 1, 9, 2, 8, 5
    ELEMENT_COUNT equ 7
    ELEMENT_SIZE  equ 8
    
    msg_sum_diff db "Сумма разниц: ", 0
    msg_avg_quot db "Среднее (целое): ", 0
    msg_avg_rem  db "Остаток: ", 0
    newline      db 0xA
    
    digit_space  db "           "
    digits_len   equ $ - digit_space
    
section .bss
    total_difference resq 1
    average_quotient resq 1
    average_remainder resq 1
    buffer resb 20
    
section .text
    global _start

%macro PRINT_STRING 1
    push    r8
    push    rcx
    push    rdx
    push    rsi
    push    rdi
    
    mov     r8, %1
    mov     rcx, 0
%%find_len:
    cmp     byte [r8], 0
    je      %%done_len
    inc     r8
    inc     rcx
    jmp     %%find_len
%%done_len:
    
    mov     rax, 1
    mov     rdi, 1
    mov     rsi, %1
    mov     rdx, rcx
    syscall
    
    pop     rdi
    pop     rsi
    pop     rdx
    pop     rcx
    pop     r8
%endmacro

%macro PRINT_NUMBER 1
    push    rdi
    push    rsi
    push    rdx
    push    rcx
    push    r8
    push    rbx
    
    mov     rax, %1
    mov     rbx, 10
    mov     rcx, buffer + 19
    mov     byte [rcx], 0xA
    mov     rdi, 19
    
    mov     r8, 0
    cmp     rax, 0
    jge     %%p_loop
    mov     r8, 1
    neg     rax
    
%%p_loop:
    xor     rdx, rdx
    idiv    rbx
    add     dl, '0'
    dec     rcx
    mov     [rcx], dl
    dec     rdi
    cmp     rax, 0
    jnz     %%p_loop
    
    cmp     r8, 1
    jne     %%p_print
    dec     rcx
    mov     byte [rcx], '-'
    dec     rdi
    
%%p_print:
    mov     rax, 1
    mov     rdi, 1
    mov     rsi, rcx
    mov     rdx, 20
    sub     rdx, rdi
    syscall
    
    pop     rbx
    pop     r8
    pop     rcx
    pop     rdx
    pop     rsi
    pop     rdi
%endmacro

_start:
    mov     rcx, ELEMENT_COUNT
    mov     rsi, array_x
    mov     rdi, array_y
    xor     r12, r12
    
loop_start:
    mov     rax, [rsi]
    mov     rbx, [rdi]
    
    sub     rax, rbx
    
    add     r12, rax
    
    add     rsi, ELEMENT_SIZE
    add     rdi, ELEMENT_SIZE
    
    loop    loop_start

    mov     [total_difference], r12
    
    mov     rax, r12
    cqo
    
    mov     rbx, ELEMENT_COUNT
    
    idiv    rbx
    
    mov     [average_quotient], rax
    mov     [average_remainder], rdx

    PRINT_STRING msg_avg_quot
    
    PRINT_NUMBER [average_quotient]
    
    PRINT_STRING msg_avg_rem
    
    PRINT_NUMBER [average_remainder]

    mov     rax, 60
    xor     rdi, rdi
    syscall