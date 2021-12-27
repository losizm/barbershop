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

  function toErrorMessage(code) {
    switch (code) {
      case 0:   return 'Could not connect to server';
      case 400: return '400 Bad Request';
      case 401: return '401 Unauthorized';
      case 403: return '403 Forbidden';
      case 404: return '404 Not Found';
      case 408: return '408 Request Timeout';
      case 413: return '413 Payload Too Large';
      case 500: return '500 Internal Server Error';
      case 501: return '501 Not Implemented';
      case 502: return '502 Bad Gateway';
      case 503: return '503 Service Unavailable';
      case 504: return '504 Gateway Timeout';

      default:
        if      (code >= 100 && code <= 199) return `${code} Informational`;
        else if (code >= 200 && code <= 299) return `${code} Successful`;
        else if (code >= 300 && code <= 399) return `${code} Redirection`;
        else if (code >= 400 && code <= 499) return `${code} Client Error`;
        else if (code >= 500 && code <= 599) return `${code} Server Error`;
        else return `${code} Unknown Error`;
    }
  }

  function loadSettings() {
    let req = new XMLHttpRequest();
    req.open('GET', '/api/comments/settings');
    req.addEventListener('load', () => {
      if (req.status >= 200 && req.status <= 299)
        setCommentTextMaxLength(JSON.parse(req.responseText));
    });
    req.send(null);
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
    removeErrorMessage();

    let req = new XMLHttpRequest();
    req.open('POST', '/api/comments', true);
    req.addEventListener('loadend', () => {
      if (req.status >= 200 && req.status <= 299) {
        resetForm();
        loadComments();
      } else {
        addErrorMessage(req.status);
      }

      setPending(false);
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

    setPending(true);
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
    let a = document.createElement('a');
    a.href = `/api/attachments/${id}`;
    a.setAttribute('download', fileName);
    a.click();
  }

  function addErrorMessage(code) {
    let message = document.createElement('div');
    message.className = 'error-message';
    message.title = 'Click to dismiss';

    if (code) {
      let image = document.createElement('img');
      image.src = 'error.svg';
      message.appendChild(image);
    } else {
      let strong = document.createElement('strong');
      strong.innerHTML = '&times;&nbsp;&nbsp;';
      message.appendChild(strong);
    }

    message.appendChild(document.createTextNode(toErrorMessage(code)));
    message.addEventListener('click', () => removeErrorMessage());

    let error = document.createElement('div');
    error.id = 'error';
    error.appendChild(message);

    let form = document.querySelector('#comments form');
    form.replaceChild(error, form.querySelector('#error'));
  }

  function removeErrorMessage() {
    let error = document.createElement('div');
    error.id = 'error';

    let form = document.querySelector('#comments form');
    form.replaceChild(error, form.querySelector('#error'));
  }

  function setPending(pending) {
    isPending = pending;

    let add  = document.querySelector('#comments form button[name="add-attachment"]');
    add.disabled = pending;

    let text = document.querySelector('#comments form input[name="text"]');
    text.disabled = pending;

    let send = document.querySelector('#comments form button[name="send"]');
    send.disabled = pending;

    let progress = document.querySelector('#progress');

    if (pending) {
      setTimeout(() => {
        if (isPending)
          progress.style.display = 'block';
      }, 1000);
    } else {
      progress.style.display = 'none';
    }
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

  function setCommentTextMaxLength(settings) {
    if (settings.textMaxLength) {
      let input = document.querySelector('#comments form input[name="text"]');
      input.maxLength = settings.textMaxLength;
    }
  }

  function setCommentList(comments) {
    function createList(comments) {
      let list = document.createElement('ul');
      list.id = 'comment-list';

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

    let main    = document.querySelector('#comments');
    let oldList = document.querySelector('#comment-list');
    let newList = createList(comments);

    main.replaceChild(newList, oldList);

    if (newList.lastChild)
      newList.lastChild.scrollIntoView(true);
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

    let form   = document.querySelector('#comments form');
    let button = form.querySelector('button[name="add-attachment"]');
    let input  = form.querySelector('input[name="attachment"]');

    button.addEventListener('click', () => {
      input.click();
      return false;
    });

    input.addEventListener('change', () => {
      let list = document.createElement('ul');
      list.id = 'attachment-list';

      for (let i = 0; i < input.files.length; i++) {
        let file     = input.files[i];
        let listItem = document.createElement('li');
        let strong   = document.createElement('strong');

        strong.innerHTML = '&times;&nbsp;&nbsp;';

        listItem.id = `attachment-file-${i}`;
        listItem.title = 'Click to remove';
        listItem.appendChild(strong);
        listItem.appendChild(document.createTextNode(`${file.name} - ${toFileSize(file.size)}`));
        listItem.addEventListener('click', () => deleteAttachment(i));

        list.appendChild(listItem);
      }

      form.replaceChild(list, form.querySelector('#attachment-list'));
      return false;
    });

    form.addEventListener('reset', () => {
      let list = document.createElement('ul');
      list.id = 'attachment-list';
      form.replaceChild(list, form.querySelector('#attachment-list'));
      return false;
    });
  }

  let isPending = false;

  addCommentHandler();
  addAttachmentHandler();
  loadSettings();
  loadComments();
})();
