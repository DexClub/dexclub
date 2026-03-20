; ============================================
; Scopes
; ============================================

; --- 顶层作用域 ---

(program) @local.scope
(class_body) @local.scope
(interface_body) @local.scope
(enum_body) @local.scope
(annotation_type_body) @local.scope

; --- 方法/构造器作用域 ---

(method_declaration) @local.scope
(constructor_declaration) @local.scope
(compact_constructor_declaration) @local.scope
(lambda_expression) @local.scope

; --- 块级作用域 ---

(block) @local.scope
(switch_block) @local.scope
(for_statement) @local.scope
(enhanced_for_statement) @local.scope
(try_statement) @local.scope
(catch_clause) @local.scope
(try_with_resources_statement) @local.scope

; ============================================
; Definitions
; ============================================

; --- 局部变量 ---

(local_variable_declaration
  declarator: (variable_declarator
    name: (identifier) @local.definition))

; --- 形参 ---

(formal_parameter
  name: (identifier) @local.definition)

(spread_parameter
  (variable_declarator
    name: (identifier) @local.definition))

; --- catch 参数 ---

(catch_formal_parameter
  name: (identifier) @local.definition)

; --- 增强 for 循环变量 ---

(enhanced_for_statement
  name: (identifier) @local.definition)

; --- try-with-resources 资源变量 ---

(resource
  name: (identifier) @local.definition)

; --- 模式变量 (Java 16+) ---

(type_pattern
  (identifier) @local.definition)

; --- Record 解构变量 (Java 21+) ---

(record_pattern_component
  (identifier) @local.definition)

; ============================================
; References
; ============================================

(identifier) @local.reference
