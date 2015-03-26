-- name: authenticate-user
select userid
from person
where userid = :userid and password = :password

-- name: user
select *
from person
where userid = :userid

-- name: iteration-stories
select s.*, tracker.initials as tracker_initials,
            customer.initials as customer_initials,
            developer.initials as developer_initials,
            t.name as team_name,
            i.short_name as epic_name,
            c.short_name as business_project_name,
            r.name as release_name
from story s left join person tracker on s.tracker_id = tracker.id
    left join person customer on s.customer_id = customer.id
    left join person developer on s.developer_id = developer.id
    left join team t on s.team_id = t.id
    left join initiative i on s.initiative_id = i.id
    left join client c on i.client_id = c.id
    left join releases r on s.release_id = r.id
where iteration_id = :iteration_id

-- name: iteration
select * from iteration
where id = :iteration_id

-- name: current-iteration
select * from iteration
where start_date <= now() and end_date >= now()
and project_id = :project_id
limit 0,1

-- name: last-iteration
select * from iteration i
where i.project_id = :project_id
and i.start_date = (select max(start_date) from iteration ii where i.project_id = ii.project_id)
limit 0,1

-- name: iteration-teams
select t.id, t.name, t.cool_name, s.estimated_hours, s.status, it.business_estimate, it.team_estimate, s.iteration_id, init.name as epic_name, init.short_name as epic_short_name
from story s join team t on s.team_id = t.id
left join iteration_team it on t.id = it.team_id and s.iteration_id = it.iteration_id
left join initiative init on s.initiative_id = init.id
where s.iteration_id = :iteration_id

-- name: team-leads
select t.id as team_id, p.*
from team_member tm, team t, person p
where tm.team_id = t.id
and tm.person_id = p.id
and tm.lead = 1
order by t.id

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