section .data
    filename    db "data.txt", 0
    
    msg_avg_quot db "Среднее (целое): ", 0
    msg_avg_rem  db "Остаток: ", 0
        
    ELEMENT_COUNT equ 7
    ELEMENT_SIZE  equ 8
    MAX_FILE_SIZE equ 256
    
section .bss
    file_descriptor resq 1
    file_buffer     resb MAX_FILE_SIZE
    
    array_x_mem     resq ELEMENT_COUNT
    array_y_mem     resq ELEMENT_COUNT
    
    total_difference resq 1
    average_quotient resq 1
    average_remainder resq 1
    
    buffer          resb 20

section .text
    global _start

%macro WRITE_STRING 1
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

%macro WRITE_NUMBER 1
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

%macro EXIT 1
    mov     rax, 60
    mov     rdi, %1
    syscall
%endmacro

%macro OPEN_FILE 2
    mov     rax, 2
    mov     rdi, %1
    mov     rsi, %2
    xor     rdx, rdx
    syscall
%endmacro

%macro READ_FILE 3
    mov     rax, 0
    mov     rdi, %1
    mov     rsi, %2
    mov     rdx, %3
    syscall
%endmacro

%macro CLOSE_FILE 1
    mov     rax, 3
    mov     rdi, %1
    syscall
%endmacro

_start:
    OPEN_FILE filename, 0
    mov     [file_descriptor], rax
    
    mov     rdi, rax
    READ_FILE rdi, file_buffer, MAX_FILE_SIZE
    
    mov     rsi, file_buffer
    mov     rdi, array_x_mem
    mov     r8, array_y_mem
    call    parse_data
    
    mov     rcx, ELEMENT_COUNT
    mov     rsi, array_x_mem
    mov     rdi, array_y_mem
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

    WRITE_STRING msg_avg_quot
    WRITE_NUMBER [average_quotient]
    
    WRITE_STRING msg_avg_rem
    WRITE_NUMBER [average_remainder]

    CLOSE_FILE [file_descriptor]
    EXIT 0
    
parse_data:
    push    rbx
    push    rcx
    push    r9
    push    r10
    
    mov     rbx, rdi
    mov     r9, r8
    mov     r10, 0
    mov     rcx, ELEMENT_COUNT * 2
    
.loop_parse:
    cmp     r10, rcx
    je      .done_parse
    
    call    parse_number
    
    cmp     r10, ELEMENT_COUNT
    jl      .save_x
    jmp     .save_y
    
.save_x:
    mov     [rbx], rax
    add     rbx, 8
    jmp     .next_element
    
.save_y:
    mov     [r9], rax
    add     r9, 8
    
.next_element:
    inc     r10
    jmp     .loop_parse
    
.done_parse:
    mov     rax, 1
    
    pop     r10
    pop     r9
    pop     rcx
    pop     rbx
    ret

parse_number:
    push    rdi
    push    rbx
    push    rcx
    push    rdx
    
    xor     rbx, rbx
    mov     rdi, 0
    mov     rdx, 0
    
.skip_whitespace:
    mov     al, [rsi]
    cmp     al, ' '
    je      .move_pointer
    cmp     al, 0xA
    je      .move_pointer
    cmp     al, 0xD
    je      .move_pointer
    cmp     al, ','
    je      .move_pointer
    jmp     .check_sign
    
.move_pointer:
    inc     rsi
    jmp     .skip_whitespace
    
.check_sign:
    mov     al, [rsi]
    cmp     al, '-'
    jne     .parse_loop
    mov     rdx, 1
    inc     rsi
    
.parse_loop:
    mov     al, [rsi]
    cmp     al, '0'
    jl      .end_parse
    cmp     al, '9'
    jg      .end_parse
    
    mov     rdi, 1
    
    sub     al, '0'
    movzx   rcx, al
    
    mov     rax, rbx
    mov     rbx, 10
    mul     rbx
    mov     rbx, rax
    add     rbx, rcx
    
    inc     rsi
    jmp     .parse_loop
    
.end_parse:
    
    cmp     rdx, 1
    jne     .positive_result
    neg     rbx
    
.positive_result:
    mov     rax, rbx
    
.return_number:
    pop     rdx
    pop     rcx
    pop     rbx
    pop     rdi
    ret