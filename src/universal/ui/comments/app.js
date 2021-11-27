/*
 * Copyright 2021 Carlos Conyers
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
(function() {
  function loadComments() {
    let req = new XMLHttpRequest();
    req.open('GET', '/api/comments');
    req.addEventListener('readystatechange', () => {
      if (req.readyState == 4 && req.status == 200) {
        let comments = JSON.parse(req.responseText);
        replaceList(comments);
      }
    });
    req.send(null);
  }

  function postComment(text) {
    let req = new XMLHttpRequest();
    req.open('POST', '/api/comments');
    req.setRequestHeader('Content-Type', 'text/plain; charset=UTF-8');
    req.addEventListener('readystatechange', () => {
      if (req.readyState == 4 && (req.status >= 200 && req.status <= 299)) {
        clearComment();
        loadComments();
      }
    });
    req.send(text);
  }

  function deleteComment(id) {
    let req = new XMLHttpRequest();
    req.open('DELETE', `/api/comments/${id}`);
    req.addEventListener('readystatechange', () => {
      if (req.readyState == 4 && (req.status >= 200 && req.status <= 299)) {
        clearComment();
        loadComments();
      }
    });
    req.send(null);
  }

  function clearComment() {
    let input = document.querySelector('section.comments > form > input');
    input.value = '';
  }

  function replaceList(comments) {
    let section = document.querySelector('section.comments')
    let list = section.querySelector('ul');
    section.replaceChild(createList(comments), list);
  }

  function createList(comments) {
    let list = document.createElement('ul');
    comments.map(comment => createListItem(comment))
      .forEach(item => list.appendChild(item));

    return list;
  }

  function createListItem(comment) {
    let time = document.createElement('div');
    time.appendChild(document.createTextNode(comment.time));
    time.className = 'time';

    let text = document.createElement('div');
    text.appendChild(document.createTextNode(comment.text));
    text.className = 'text';
    text.title = 'Click to delete'
    text.addEventListener('click', () => deleteComment(comment.id));

    let listItem = document.createElement('li');
    listItem.appendChild(time);
    listItem.appendChild(text);

    return listItem;
  }

  function addFormHandler() {
    let form = document.querySelector('section.comments > form');
    let input = form.querySelector('input');

    form.onsubmit = () => {
      let value = input.value.trim();
      input.value = value;

      if (value !== '')
        postComment(value);

      return false;
    };
  }

  addFormHandler();
  loadComments();
})()
