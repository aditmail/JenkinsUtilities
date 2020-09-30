package com.jenkins.util.checker.utils

interface IConfig {

    interface PrintWriter {
        fun pwLine(value: String?)
        fun pw(value: String)
    }

    interface StringBuilders {
        fun stbAppend(value: String?)
        fun stbAppendStyle(model: String?, value: String?)
        fun stbAppendTableHeader(value: String?, arrValue: MutableList<Any>?)
        fun stbAppendTableData(value: String?, arrValue: MutableList<Any>?)
    }
}