; ============================================
; Definitions
; ============================================

; --- 类/接口/枚举/记录/注解类型声明 ---

(class_declaration
  name: (identifier) @name) @definition.class

(interface_declaration
  name: (identifier) @name) @definition.interface

(enum_declaration
  name: (identifier) @name) @definition.class

(record_declaration
  name: (identifier) @name) @definition.class

(annotation_type_declaration
  name: (identifier) @name) @definition.class

; --- 方法/构造器声明 ---

(method_declaration
  name: (identifier) @name) @definition.method

(constructor_declaration
  name: (identifier) @name) @definition.method

(compact_constructor_declaration
  name: (identifier) @name) @definition.method

(annotation_type_element_declaration
  name: (identifier) @name) @definition.method

; --- 字段/常量/枚举常量声明 ---

(field_declaration
  declarator: (variable_declarator
    name: (identifier) @name)) @definition.field

(constant_declaration
  declarator: (variable_declarator
    name: (identifier) @name)) @definition.field

(enum_constant
  name: (identifier) @name) @definition.field

; ============================================
; References
; ============================================

; --- 方法调用/方法引用 ---

(method_invocation
  name: (identifier) @name
  arguments: (argument_list) @reference.call)

(method_reference) @reference.call

; --- 类型引用 ---

(object_creation_expression
  type: (type_identifier) @name) @reference.class

(superclass
  (type_identifier) @name) @reference.class

(type_list
  (type_identifier) @name) @reference.implementation

; --- 注解类型引用 ---

(marker_annotation
  name: (identifier) @name) @reference.class

(annotation
  name: (identifier) @name) @reference.class

; --- 构造器调用引用 ---

(explicit_constructor_invocation
  (super)) @reference.call

(explicit_constructor_invocation
  (this)) @reference.call

; --- 字段访问 ---

(field_access
  field: (identifier) @name) @reference.field
