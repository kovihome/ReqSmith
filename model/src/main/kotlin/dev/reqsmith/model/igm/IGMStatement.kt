/*
 * ReqSmith - Build application from requirements
 * Copyright (c) 2025-2026. Kovi <kovihome86@gmail.com>
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package dev.reqsmith.model.igm

enum class IGMStatement {
    /**
     * The `print` statement is used to output a message or value to the console or standard output.
     * The content of the `print` statement can be a string literal, a variable, or an expression that evaluates to a value.
     */
    print,
    /**
     * The `call` statement is used to invoke a function or method.
     * It allows you to execute a block of code defined in a function or method, and optionally pass arguments to it.
     */
    call,
    /**
     * The `set` statement is used to assign a value to a variable.
     * It allows you to create or update a variable with a specific value, which can be a string literal, a number, a boolean, or the result of an expression.
     */
    `set`,
    /**
     * The `return` statement is used to exit a function or method and optionally return a value to the caller.
     * It allows you to specify the value that should be returned when the function or method is called,
     * and it also indicates that the execution of the function or method should stop at that point.
     */
    `return`,
    /**
     * Native statement is used to include native code in the generated output.
     * The content of the statement is expected to be the native code itself.
     * The handling of this statement is specific to the target platform and may involve direct insertion
     * of the native code into the generated output without any modification or interpretation.
     * This allows for greater flexibility and control over the generated code, enabling developers to leverage
     * platform-specific features or optimizations that may not be directly supported by the IGM language.
     * The use of native statements should be done with caution, as it can lead to platform-specific dependencies
     * and may require additional handling to ensure compatibility across different target platforms.
     */
    native
}