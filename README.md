# Eulerity TaskManager

### Setup and Run
   - Prerequisites: Java 17, Maven wrapper included
   - Single command: `./mvnw spring-boot:run`
   - Test cases: `./mvnw test`
   - Set OPENAI\_API\_KEY env variable for AI endpoint
   - H2 console at `http://localhost:8080/h2-console`. DB credentials can be set as env vars or use defaults from application properties

   
### AI Endpoint

- I selected tasks suggestion as I can scope it in the limited time frame and build better orchestration around it. 
- It takes user query in natural language and returns proper Task structure schema.  
- Safety: first the user query is sanitized and passed through lightweight LLM to check for prompt injection and any harmful request. This will decide whether we need to reject the query with custom AISuggestionError.
- If the safety passes then it goes to the main LLM which returns structured json which is validated with TaskResponseDTO which is same when we create or update the task.
- Prompts are XML based to enforce structure and add content dynamically
- Caching: Created H2 cache layer for AI response to avoid expensive model calling and avoid user spamming the request. 
- Future: Add semantic caching using the query embedding with proper threshold. Also I would implement task quering using natural language as well. 


POST /tasks/suggest

Request 

```
{
    "query": "remind me to prepare the quarterly sales report for Q2, need to include revenue breakdown and regional performance by saturday"
}
```
Response:
It is the form of TaskResponseDTO and will populate the UI create task fields so that user can decide to amend anything or call create task api. 

```
{
    "description": "Include revenue breakdown and regional performance",
    "dueDate": "2026-08-29",
    "priority": "MEDIUM",
    "projectId": null,
    "status": "TODO",
    "title": "Prepare the quarterly sales report for Q2"
}
```

## Design Decisions

**Entity Modeling :**

- Task entity is same as suggested by the assignment. When a new task is created the status I am defaulting is TODO and user should then use update to change the status. This is deliberate as it creates proper audit trail of new task lifecycle which should start with TODO not IN_PROGRES or DONE.
- In addition the relational entity I chose is Project. Tasks can be attached or detached from a project using update task request. 
- If someone deletes a project then it blocks the operation until all the tasks are disassociated. I chose to block deletion rather than cascade because silently deleting tasks felt dangerous, and setting them to null felt like hiding the problem.

**Pagination/Filtering :**

- I chose page/size as its more intuitive for client and properly scoped for a taskmanager showing task list.
- As of now it doesn't handle concurrent mutation which will cause the shift. 
- Future: I will implement cursor based using task-id as key which is immune to any mutation. 

**Exception Handling :**

- All errors return a consistent envelope: `{ "error": "ERROR_CODE", "message": "human readable reason", "fields": [contains list of field issue due to missing/validatoin failure] }` 
- Validation errors populate the fields array so clients can map errors back to specific inputs. Single errors use message only. 
- The shape is identical at the top level regardless of error type. 
- If there is any unexpected error then it goes into generic exception stating internal server issue.

**AI Model failure handling :**

- AI calls are timeout based retries for errors which may come either if model's output doesn't match the required structure or api call fails.
- It follows same error structure defined throughout the application and goes through custom global exception handler with its type as AiSuggestionException.
- Safety LLM call also follows same envelop for harmful queries. 
- Upstream error message are not passed to client as they might leak information. These errors are logged internally for debugging. 

**Task Audit Trail (Stretch feature) :**

- I chose to create Task Audit trail as it is most applicable to real world use cases.
- Whenever task is created/updated/deleted, it will log it into task\_audit\_log table 
- Task audit log entity uses just taskId as Long instead of ManyToOne because audit should survive the task deletion.
- Any failure in auditing doesn't stop the normal failure as its not part of the main business logic but a history trail. 
- I didn't add delete operation of audit as the trail needs history.
- Creation and deletion audits are simple but update is little tricky. We need to check which field changed and the log it. Instead of creating separate entry for each field, I am making a list of field changes which is stored as serialized json into db.
- Also it helped me make the update request idempotent. So if there is no field changed then we don't do any update db call. 
- Created one api endpoint tasks/{id}/history which returns complete audit trail for the task. There is no validation on the id here because if the task is deleted then we shouldn't return error. So for any invalid id, it will just return empty audit trail list. 
- Future: I will add full snapshot of task in the trail, make the audit log async so it doesn't stop the main flow, handle concurrent updates preventing two simultaneous updates from silently overwriting each other, which would produce a misleading audit trail. 











