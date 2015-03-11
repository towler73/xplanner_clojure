-- name: authenticate-user
select userid
from person
where userid = :userid and password = :password

-- name: user
select *
from person
where userid = :userid

-- name: iteration-stories
select s.*, tracker.initials as tracker_initials, customer.initials as customer_initials, developer.initials as developer_initials, t.name as team_name
from story s left join person tracker on s.tracker_id = tracker.id
    left join person customer on s.customer_id = customer.id
    left join person developer on s.developer_id = developer.id
    left join team t on s.team_id = t.id
where iteration_id = :iteration_id

-- name: iteration
select * from iteration
where id = :iteration_id

-- name: iteration-teams
select t.id, t.name, t.cool_name, s.estimated_hours, s.status, it.business_estimate, it.team_estimate, s.iteration_id
from story s join team t on s.team_id = t.id
left join iteration_team it on t.id = it.team_id and s.iteration_id = it.iteration_id
where s.iteration_id = :iteration_id

-- name: insert-team-estimate!
insert into iteration_team (iteration_id, team_id, team_estimate)
values (:iteration_id, :team_id, :estimate)

-- name: update-team-estimate!
update iteration_team set team_estimate = :team_estimate
where iteration_id = :iteration_id and team_id = :team_id

-- name: select-iteration-team
select *
from iteration_team
where iteration_id = :iteration_id and team_id = :team_id

-- name: project-iterations
select *
from iteration
where project_id = :project_id
order by start_date desc

-- name: person
select * from person
where id = :person_id

-- name: team
select * from team
where id = :team_id