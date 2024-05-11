// Licensed to the Apache Software Foundation (ASF) under one
// or more contributor license agreements.  See the NOTICE file
// distributed with this work for additional information
// regarding copyright ownership.  The ASF licenses this file
// to you under the Apache License, Version 2.0 (the
// "License"); you may not use this file except in compliance
// with the License.  You may obtain a copy of the License at
//
//   http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing,
// software distributed under the License is distributed on an
// "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
// KIND, either express or implied.  See the License for the
// specific language governing permissions and limitations
// under the License.

// Define a function named `add` that takes two `usize` parameters and returns a `usize` result
pub fn add(left: usize, right: usize) -> usize {
    // Add the left and right parameters together and return the result
    left + right
}

// Define a module named `tests` that contains unit tests for the `add` function
#[cfg(test)]
mod tests {
    // Import items from the outer scope (including the `add` function)
    use super::*;

    // Define a unit test named `it_works` that checks if the `add` function produces the expected result
    #[test]
    fn it_works() {
        // Call the `add` function with parameters 2 and 2
        let result = add(2, 2);

        // Assert that the result is equal to 4
        assert_eq!(result, 4);
    }
}

