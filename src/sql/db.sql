-- name: iteration-stories
select * from story
where iteration_id = :iteration_id

-- name: iteration
select * from iteration
where id = :iteration_id

-- name: iteration-teams
select * from story s join team t on s.team_id = t.id
where iteration_id = :iteration_id