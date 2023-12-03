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

use reqwest;

#[tokio::main]
async fn main() -> Result<(), reqwest::Error> {
    // The URL of the server you want to visit
    let url = "https://example.com";

    // Make a GET request to the server
    let response = reqwest::get(url).await?;

    // Check if the request was successful (status code 200)
    if response.status().is_success() {
        // Print the response body as text
        let body = response.text().await?;
        println!("Response body:\n{}", body);
    } else {
        // Print the status code and reason phrase for unsuccessful requests
        println!("Request failed with status: {} - {}", response.status(), response.status().canonical_reason().unwrap_or("Unknown"));
    }

    Ok(())
}
