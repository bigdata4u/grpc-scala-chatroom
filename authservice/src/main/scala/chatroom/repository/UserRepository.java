/*
 * Copyright 2017 Google Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package chatroom.repository;

import chatroom.domain.User;

import java.io.*;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;

/**
 * Created by rayt on 6/27/17.
 */
public class UserRepository {

  private static Logger logger = Logger.getLogger(UserRepository.class.getName());

  private static final String FILENAME = "./userdatabase.txt";

  private ConcurrentHashMap<String, User> users = new ConcurrentHashMap<>();

  public UserRepository() {
    try {
      readUserDatabase();
    } catch (Exception e) {
      e.printStackTrace();
    }

  }

  public User findUser(String username) {
    User user = users.get(username);
    if (user == null) return null;

    return user;
  }

  public Iterable<String> findRoles(String username) {
    User user = findUser(username);
    if (user == null) {
      return Collections.emptySet();
    }
    return  user.rolesAsJavaSet();
  }

  public User save(User user) {
    User copy = User.copyUser(user);
    users.put(user.username(), copy);
    try {
      saveUserDatabase();
    } catch (Exception e) {
      logger.info("Error saving user database");
    }
    return copy;
  }

  /**
   * Just a real simple database that overwrites the entire file every time it gets written out to disk.
   * It has the format of username, password, role a, role b, ....
   */
  private void saveUserDatabase() throws Exception {

    File file = getOrCreateFile();
    FileWriter fw = new FileWriter(file);

    try (BufferedWriter bw = new BufferedWriter(fw)) {
      users.values().forEach(user -> {
        try {
          bw.write(user.toString() + "\n");
        } catch (IOException e) {
          e.printStackTrace();
        }
      });
      System.out.println("Finished writing user database");
    } catch (IOException e) {
      e.printStackTrace();
    }
  }

  private File getOrCreateFile() throws Exception {
    File file = new File(FILENAME);
    if (!file.exists()) {
      boolean createdFile = file.createNewFile();
      if (!createdFile)
        throw new Exception("Could not create file");
    }
    return file;
  }


  //Make wild assumptions and hope file format is a list of strings in the format of username,password, role a, role b
  private void readUserDatabase() throws Exception {
    File file = getOrCreateFile();

    try (BufferedReader br = new BufferedReader(new FileReader(file))) {
      br.lines().forEach(nextUser -> {
        String[] userPasswordAndRoles = nextUser.split(",");
        if (userPasswordAndRoles.length < 2) {
          throw new RuntimeException("Invalid database format.");
        }
        Set<String> roles = new LinkedHashSet<>();
        roles.addAll(Arrays.asList(userPasswordAndRoles).subList(2, userPasswordAndRoles.length));
        User user = User.userFromJava(userPasswordAndRoles[0], userPasswordAndRoles[1],roles);
        users.put(user.username(), user);
        logger.info("added user: " + user.username());
      });
      System.out.println("Finished reading user database");
    } catch (IOException e) {
      e.printStackTrace();
    }
  }

}
