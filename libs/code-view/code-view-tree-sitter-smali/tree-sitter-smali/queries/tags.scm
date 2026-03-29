; ============================================
; Definitions
; ============================================

; --- 类声明 ---

(class_directive
  (class_identifier) @name) @definition.class

; --- 方法定义 ---

(method_definition
  (method_signature
    (method_identifier) @name)) @definition.method

; --- 字段定义 ---

(field_definition
  (field_identifier) @name) @definition.field

; ============================================
; References
; ============================================

; --- 超类引用 ---

(super_directive
  (class_identifier) @name) @reference.class

; --- 接口实现引用 ---

(implements_directive
  (class_identifier) @name) @reference.implementation

; --- 注解类型引用 ---

(annotation_directive
  (class_identifier) @name) @reference.class

; --- 方法调用引用 ---

(full_method_signature
  (method_signature
    (method_identifier) @name)) @reference.call

; --- 字段访问引用 ---

(body
  (class_identifier)
  (field_identifier) @name) @reference.field
