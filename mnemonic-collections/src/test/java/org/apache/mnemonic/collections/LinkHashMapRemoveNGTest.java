/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */


import java.util.LinkedHashMap;
import org.testng.annotations.Test;
import org.testng.AssertJUnit;


public class LinkHashMapRemoveNGTest {

@Test

   public static void testLinkHashMapRemove() {
      // Create a hash map
       LinkedHashMap<String, Double> lhm = new LinkedHashMap<String, Double>();
      // Put elements to the map
      lhm.put("Zara", new Double(100.00));
      lhm.put("Mahnaz", new Double(200.00));
      lhm.put("Ayan", new Double(300.00));
      lhm.put("Daisy", new Double(400.00));
      lhm.put("Qadir", new Double(500.00));
      
      
      lhm.remove("Zara");
     //  System.out.println("Remove Zara");
      double total = 0;
      for (String key : lhm.keySet()) {

          System.out.println(lhm.get(key));
          total += lhm.get(key);
          
        }         
     //  System.out.println("the total is " + total);
       
      AssertJUnit.assertEquals(1400.0, total);     

    
   }
}
