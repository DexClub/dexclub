; ============================================
; Scopes
; ============================================

(source_file) @local.scope

(class_declaration) @local.scope
(object_declaration) @local.scope
(companion_object) @local.scope
(anonymous_initializer) @local.scope

(function_declaration) @local.scope
(secondary_constructor) @local.scope
(anonymous_function) @local.scope
(getter) @local.scope
(setter) @local.scope
(lambda_literal) @local.scope

(for_statement) @local.scope
(catch_block) @local.scope
(finally_block) @local.scope
(when_expression) @local.scope
(control_structure_body) @local.scope

; ============================================
; Definitions
; ============================================

; --- 函数 / setter 参数 ---
;
; Kotlin 的 primary constructor parameter 作用域不等同于普通 lexical local：
; 1. `val/var` 参数本质上是 property
; 2. 普通参数也不能简单视为整个 class body 可见
;
; 先不把 class_parameter 记进 locals，避免误把它泄漏到成员函数里。

(parameter
  (simple_identifier) @local.definition)

(parameter_with_optional_type
  (simple_identifier) @local.definition)

; --- lambda 参数 ---

(lambda_literal
  (lambda_parameters
    (variable_declaration
      (simple_identifier) @local.definition)))

(lambda_literal
  (lambda_parameters
    (multi_variable_declaration
      (variable_declaration
        (simple_identifier) @local.definition))))

; --- 块级 val / var 定义 ---

(statements
  (property_declaration
    (variable_declaration
      (simple_identifier) @local.definition)))

(statements
  (property_declaration
    (multi_variable_declaration
      (variable_declaration
        (simple_identifier) @local.definition))))

(control_structure_body
  (property_declaration
    (variable_declaration
      (simple_identifier) @local.definition)))

(control_structure_body
  (property_declaration
    (multi_variable_declaration
      (variable_declaration
        (simple_identifier) @local.definition))))

; --- 局部函数定义 ---

(statements
  (function_declaration
    (simple_identifier) @local.definition))

(control_structure_body
  (function_declaration
    (simple_identifier) @local.definition))

; --- for 循环变量 ---

(for_statement
  (variable_declaration
    (simple_identifier) @local.definition))

(for_statement
  (multi_variable_declaration
    (variable_declaration
      (simple_identifier) @local.definition)))

; --- when (val x = expr) ---

(when_subject
  (variable_declaration
    (simple_identifier) @local.definition))

; --- catch (e: Throwable) ---

(catch_block
  (simple_identifier) @local.definition)

; ============================================
; References
; ============================================

; 只在表达式位置捕获引用，避免把 package/import/type/member name 一并算成 local reference。

(statements
  (simple_identifier) @local.reference)

(control_structure_body
  (simple_identifier) @local.reference)

(property_declaration
  (simple_identifier) @local.reference)

(assignment
  (simple_identifier) @local.reference)

(directly_assignable_expression
  (simple_identifier) @local.reference)

(jump_expression
  (simple_identifier) @local.reference)

(when_subject
  (simple_identifier) @local.reference)

(when_condition
  (simple_identifier) @local.reference)

(if_expression
  (simple_identifier) @local.reference)

(while_statement
  (simple_identifier) @local.reference)

(do_while_statement
  (simple_identifier) @local.reference)

(for_statement
  (simple_identifier) @local.reference)

(value_argument
  . (simple_identifier) @local.reference .)

(value_argument
  (simple_identifier)
  (simple_identifier) @local.reference)

(parenthesized_expression
  (simple_identifier) @local.reference)

(collection_literal
  (simple_identifier) @local.reference)

(call_expression
  (simple_identifier) @local.reference)

(navigation_expression
  (simple_identifier) @local.reference)

(indexing_expression
  (simple_identifier) @local.reference)

(callable_reference
  member: (simple_identifier) @local.reference)

(prefix_expression
  (simple_identifier) @local.reference)

(postfix_expression
  (simple_identifier) @local.reference)

(spread_expression
  (simple_identifier) @local.reference)

(as_expression
  (simple_identifier) @local.reference)

(multiplicative_expression
  (simple_identifier) @local.reference)

(additive_expression
  (simple_identifier) @local.reference)

(range_expression
  (simple_identifier) @local.reference)

(infix_expression
  (simple_identifier) @local.reference)

(elvis_expression
  (simple_identifier) @local.reference)

(check_expression
  (simple_identifier) @local.reference)

(comparison_expression
  (simple_identifier) @local.reference)

(equality_expression
  (simple_identifier) @local.reference)

(conjunction_expression
  (simple_identifier) @local.reference)

(disjunction_expression
  (simple_identifier) @local.reference)

(interpolated_identifier) @local.reference

(interpolated_expression
  (simple_identifier) @local.reference)
