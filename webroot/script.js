document.getElementById('addTask').addEventListener('click', () => {
    const taskInput = document.getElementById('newTask');
    const task = taskInput.value.trim();
    if (task) {
        fetch('/api/todos', {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json'
            },
            body: JSON.stringify({ task: task })
        })
        .then(response => response.json())
        .then(result => {
            console.log('Result from addTask:', result);
            if (result.status === 'success') {
                showMessage('Tarea agregada exitosamente');
                taskInput.value = '';
            } else {
                showMessage('Error al agregar la tarea.');
            }
        })
        .catch(error => {
            console.error('Error:', error);
            showMessage('Error en la conexión al servidor.');
        });
    } else {
        showMessage('Por favor, ingresa una tarea.');
    }
});

document.getElementById('fetchTodos').addEventListener('click', () => {
    fetch('/api/todos')
        .then(response => response.json())
        .then(data => {
            console.log('Data from fetchTodos:', data);
            if (data.todos && data.todos.length > 0) {
                const tasksList = data.todos.map(task => `- ${task}`).join('\n');
                showTaskList(`Tareas:\n${tasksList}`);
            } else {
                showTaskList('No hay tareas pendientes.');
            }
        })
        .catch(error => {
            console.error('Error:', error);
            showMessage('Error en la conexión al servidor.');
        });
});

document.getElementById('clearTodos').addEventListener('click', () => {
    fetch('/api/todos/clear', {
        method: 'POST',
    })
    .then(response => response.json())
    .then(result => {
        console.log('Result from clearTodos:', result);
        if (result.status === 'success') {
            showMessage('Todas las tareas borradas');
        } else {
            showMessage('Error al borrar las tareas.');
        }
    })
    .catch(error => {
        console.error('Error:', error);
        showMessage('Error en la conexión al servidor.');
    });
});

document.getElementById('fetchImage').addEventListener('click', () => {
    const resultDiv = document.getElementById('result');
    const existingImage = resultDiv.querySelector('img');

    if (existingImage) {
        resultDiv.removeChild(existingImage);
    } else {
        fetch('/api/paisaje')
            .then(response => response.blob())
            .then(blob => {
                const url = URL.createObjectURL(blob);
                const img = document.createElement('img');
                img.src = url;
                resultDiv.innerHTML = '';
                resultDiv.appendChild(img);
            })
            .catch(error => {
                console.error('Error:', error);
                showMessage('Error al cargar la imagen.');
            });
    }
});

function showTaskList(tasks) {
    const modal = document.getElementById('taskModal');
    const taskList = document.getElementById('taskList');
    taskList.textContent = tasks;
    modal.style.visibility = 'visible';
    modal.style.opacity = '1';
}


function showMessage(message) {
    const modal = document.getElementById('messageModal');
    const modalMessage = document.getElementById('modalMessage');
    modalMessage.textContent = message;
    modal.style.visibility = 'visible';
    modal.style.opacity = '1';
}

document.querySelectorAll('.close').forEach(closeButton => {
    closeButton.addEventListener('click', () => {
        const modal = closeButton.closest('.modal');
        modal.style.visibility = 'hidden';
        modal.style.opacity = '0';
    });
});

window.addEventListener('click', (event) => {
    const modals = document.querySelectorAll('.modal');
    modals.forEach(modal => {
        if (event.target === modal) {
            modal.style.visibility = 'hidden';
            modal.style.opacity = '0';
        }
    });
});