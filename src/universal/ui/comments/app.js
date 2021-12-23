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
(function initApp() {
  function toFileSize(size) {
    const KiB = 1024;
    const MiB = KiB * 1024;
    const GiB = MiB * 1024;

    if (size >= GiB)
      return `${Math.round(size / GiB)} GiB`;
    else if (size >= MiB)
      return `${Math.round(size / MiB)} MiB`;
    else if (size >= KiB)
      return `${Math.round(size / KiB)} KiB`;
    else
      return `< 1 KiB`;
  }

  function loadComments() {
    let req = new XMLHttpRequest();
    req.open('GET', '/api/comments');
    req.addEventListener('load', () => {
      if (req.status >= 200 && req.status <= 299)
        setCommentList(JSON.parse(req.responseText));
    });
    req.send(null);
  }

  function postComment(text, attachments) {
    let req = new XMLHttpRequest();
    req.open('POST', '/api/comments', true);
    req.addEventListener('load', () => {
      if (req.status >= 200 && req.status <= 299) {
        resetForm();
        loadComments();
      }
    });

    if (attachments.length) {
      let formData = new FormData();
      formData.append('text', text);
      attachments.forEach(file => {
        if (!file.removed)
          formData.append('attachment', file);
      });
      req.send(formData);
    }
    else {
      req.setRequestHeader('Content-Type', 'text/plain; charset=UTF-8');
      req.send(text);
    }
  }

  function deleteComment(id) {
    if (confirm('Do you want to delete this comment?')) {
      let req = new XMLHttpRequest();
      req.open('DELETE', `/api/comments/${id}`);
      req.addEventListener('load', () => {
        if (req.status >= 200 && req.status <= 299)
          loadComments();
      });
      req.send(null);
    }
  }

  function downloadAttachment(id, fileName) {
    let a = document.createElement("a");
    a.href = `/api/attachments/${id}`;
    a.setAttribute("download", fileName);
    a.click();
  }

  function resetForm() {
    let form = document.querySelector('#comments form');
    form.reset();
  }

  function getAttachments() {
    let input = document.querySelector('#comments form input[name="attachment"]');
    let files = [];

    for (let i = 0; i < input.files.length; i++)
      if (!input.files[i].removed)
        files.push(input.files[i]);

    return files;
  }

  function getComment() {
    return document.querySelector('#comments form input[name="text"]').value;
  }

  function setComment(text) {
    document.querySelector('#comments form input[name="text"]').value = text;
  }

  function setCommentList(comments) {
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
      text.title = 'Click to delete';
      text.addEventListener('click', () => deleteComment(comment.id));

      let files = comment.attachments.map((file) => {
        let image = document.createElement('img');
        image.src = 'download.svg';

        let attachment = document.createElement('div');
        attachment.appendChild(image);
        attachment.appendChild(document.createTextNode(`${file.name} - ${toFileSize(file.size)}`));
        attachment.className = 'file';
        attachment.title = 'Click to download';
        attachment.addEventListener('click', () => downloadAttachment(file.id, file.name));
        return attachment;
      });

      let listItem = document.createElement('li');
      listItem.id = 'comment-' + comment.id;
      listItem.appendChild(time);
      listItem.appendChild(text);

      if (files.length) {
        let attachments = document.createElement('div');
        attachments.className = 'attachments';

        files.forEach(file => attachments.appendChild(file));
        listItem.appendChild(attachments);
      }

      return listItem;
    }

    let section = document.querySelector('#comments');
    let list    = section.querySelector('ul');
    section.replaceChild(createList(comments), list);
  }

  function addCommentHandler() {
    let form = document.querySelector('#comments form');

    form.onsubmit = () => {
      let text = getComment().trim();
      form.querySelector('input[name="text"]').value = text;

      if (text !== '')
        postComment(text, getAttachments());

      return false;
    };
  }

  function addAttachmentHandler() {
    function deleteAttachment(index) {
      let list = document.querySelector('#attachment-list');
      list.removeChild(document.querySelector(`#attachment-file-${index}`));

      input.files[index].removed = true;
    }

    let form   = document.querySelector('#comments > form');
    let button = form.querySelector('button[name="add-attachment"]');
    let input  = form.querySelector('input[name="attachment"]');

    button.onclick = () => {
      input.click();
      return false;
    };

    input.onchange = () => {
      let list = document.createElement('section');
      list.id = 'attachment-list';

      for (let i = 0; i < input.files.length; i++) {
        let file     = input.files[i];
        let listItem = document.createElement('div');
        let span     = document.createElement('span');

        span.innerHTML = '<strong>x</strong>&nbsp;&nbsp;';

        listItem.id = `attachment-file-${i}`;
        listItem.className = 'file-item';
        listItem.title = 'Click to remove';
        listItem.appendChild(span);
        listItem.appendChild(document.createTextNode(`${file.name} - ${toFileSize(file.size)}`));
        listItem.addEventListener('click', () => deleteAttachment(i));

        list.appendChild(listItem);
      }

      form.replaceChild(list, form.querySelector("#attachment-list"));
      return false;
    };

    form.addEventListener('reset', () => {
      let list = document.createElement('section');
      list.id = 'attachment-list';
      form.replaceChild(list, form.querySelector("#attachment-list"));
      return false;
    });
  }

  addCommentHandler();
  addAttachmentHandler();
  loadComments();
})();
