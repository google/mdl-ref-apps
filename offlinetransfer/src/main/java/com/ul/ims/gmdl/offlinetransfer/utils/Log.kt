/*
 * Copyright (C) 2019 Google LLC
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
*/

package com.ul.ims.gmdl.offlinetransfer.utils

import android.util.Log
import kotlin.math.min


object Log {
    private const val CHUNK_SIZE = 2048

    fun d(tag : String, message : String) {
        var i = 0
        while (i < message.length) {
            Log.d(tag, message.substring(i, min(message.length, i + CHUNK_SIZE)))
            i += CHUNK_SIZE
        }
    }
}