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
select * from story s join team t on s.team_id = t.id
where iteration_id = :iteration_id

-- name: person
select * from person
where id = :person_id

-- name: team
select * from team
where id = :team_id